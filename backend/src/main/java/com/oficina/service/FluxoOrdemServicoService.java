package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.model.DecisaoCliente;
import com.oficina.model.ExecucaoServico;
import com.oficina.model.ItemOs;
import com.oficina.model.StatusExecucaoServico;
import com.oficina.model.Funcionario;
import com.oficina.model.ItemOrcamento;
import com.oficina.model.Notificacao;
import com.oficina.model.Orcamento;
import com.oficina.model.OrdemServico;
import com.oficina.model.Peca;
import com.oficina.model.StatusFuncionario;
import com.oficina.model.StatusOs;
import com.oficina.model.TipoItemOrcamento;
import com.oficina.model.TipoNotificacao;
import com.oficina.model.Usuario;
import com.oficina.model.Veiculo;
import com.oficina.repository.FuncionarioRepository;
import com.oficina.repository.ExecucaoServicoRepository;
import com.oficina.repository.ItemOsRepository;
import com.oficina.repository.ItemOrcamentoRepository;
import com.oficina.repository.NotificacaoRepository;
import com.oficina.repository.OrcamentoRepository;
import com.oficina.repository.OrdemServicoRepository;
import com.oficina.repository.PecaRepository;
import com.oficina.repository.UsuarioRepository;
import com.oficina.repository.VeiculoRepository;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parte 4, etapa 1: recepcao -> avaliacao -> precificacao -> gerente -> cliente.
 * Orcar PECA NAO grava item_os e NAO baixa estoque. Essa acao sera feita na etapa de execucao.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FluxoOrdemServicoService {
    private final OrdemServicoRepository ordens;
    private final VeiculoRepository veiculos;
    private final FuncionarioRepository funcionarios;
    private final UsuarioRepository usuarios;
    private final PecaRepository pecas;
    private final OrcamentoRepository orcamentos;
    private final ItemOrcamentoRepository itens;
    private final NotificacaoRepository notificacoes;
    private final AuditoriaService auditoria;
    private final ExecucaoServicoRepository execucoes;
    private final ItemOsRepository itensOs;

    public record ItemAvaliacao(TipoItemOrcamento tipo, String descricao, Long pecaId, Integer quantidade) { }
    public record PrecoItem(Long itemId, BigDecimal valorUnitario) { }

    /** Retornos administrativos: todas as etapas do fluxo de orcamento atualmente implementado. */
    public enum EtapaCorrecao { ABERTURA, AVALIACAO, PRECIFICACAO, APROVACAO_GERENTE, RESPOSTA_CLIENTE }

    private static final Set<StatusOs> ETAPAS_CORRIGIVEIS = EnumSet.of(
            StatusOs.AGUARDANDO_AVALIACAO, StatusOs.EM_AVALIACAO,
            StatusOs.AGUARDANDO_PRECIFICACAO, StatusOs.AGUARDANDO_APROVACAO_GERENTE,
            StatusOs.AGUARDANDO_CLIENTE, StatusOs.REVISAO_ORCAMENTO,
            StatusOs.ORCAMENTO_RECUSADO, StatusOs.AGUARDANDO_PECAS,
            StatusOs.AGUARDANDO_EXECUCAO);


    @Transactional(readOnly = true)
    public List<OrdemServico> listar(UsuarioPrincipal ator) {
        validarAutenticacao(ator);
        if (ator.isGerenteOuAdmin() || ator.isAssistente()) {
            return ordens.findAllByOrderByDataAberturaDesc();
        }
        if (ator.isMecanico()) {
            Funcionario vinculo = funcionarios.findByUsuarioId(ator.getId())
                    .orElseThrow(() -> new AccessDeniedException("Mecânico sem vínculo com funcionário."));
            return ordens.findByFuncionarioIdOrderByDataAberturaDesc(vinculo.getId());
        }
        throw new AccessDeniedException("Perfil sem acesso às ordens de serviço.");
    }

    @Transactional(readOnly = true)
    public OrdemServico buscar(Long id, UsuarioPrincipal ator) {
        OrdemServico os = buscarOs(id);
        validarLeitura(os, ator);
        return os;
    }

    @Transactional(readOnly = true)
    public List<Orcamento> historicoOrcamentos(Long osId, UsuarioPrincipal ator) {
        buscar(osId, ator);
        return orcamentos.findByOsIdOrderByVersaoDesc(osId);
    }

    @Transactional(readOnly = true)
    public List<ItemOrcamento> listarItens(Long osId, Long orcamentoId, UsuarioPrincipal ator) {
        OrdemServico os = buscar(osId, ator);
        validarOrcamento(os, orcamentoId);
        return itens.findByOrcamentoIdOrderByIdAsc(orcamentoId);
    }

    public OrdemServico abrir(Long veiculoId, String problema, UsuarioPrincipal ator) {
        validarRecepcaoOuGestao(ator);
        if (problema == null || problema.isBlank() || problema.length() > 4000) {
            throw new NegocioException("Informe a descrição do problema (até 4.000 caracteres).");
        }
        Veiculo veiculo = veiculos.findById(veiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado."));
        OrdemServico os = new OrdemServico();
        os.setNumero("OS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        os.setVeiculo(veiculo);
        os.setProblema(problema.trim());
        os.setStatus(StatusOs.AGUARDANDO_AVALIACAO);
        // Nova OS começa sem prazo; não reaproveita a previsão de uma OS anterior.
        veiculo.setPrevisaoEntrega(null);
        os.setDataAbertura(LocalDateTime.now());
        os = ordens.save(os);
        auditoria.registrar(ator, "CRIAR", "ordem_servico", os.getId(), "OS aberta para avaliação");
        return os;
    }

    public OrdemServico atribuirMecanico(Long osId, Long funcionarioId, UsuarioPrincipal ator) {
        validarGestao(ator);
        OrdemServico os = buscarOs(osId);
        if (os.getStatus() != StatusOs.AGUARDANDO_AVALIACAO && os.getStatus() != StatusOs.EM_AVALIACAO
                && !(ator.isAdmin() && os.getStatus() == StatusOs.REVISAO_ORCAMENTO)) {
            throw new NegocioException("A OS não está na etapa de atribuição de mecânico.");
        }
        Funcionario funcionario = funcionarios.findById(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado."));
        if (funcionario.getStatus() != StatusFuncionario.ATIVO || funcionario.getUsuario() == null
                || !funcionario.getUsuario().isAtivo()
                || funcionario.getUsuario().isBloqueado()
                || !Perfis.MECANICO.equals(funcionario.getUsuario().getFuncao().getNome())) {
            throw new NegocioException("Selecione um mecânico ativo com acesso ao sistema.");
        }
        os.setFuncionario(funcionario);
        if (os.getStatus() != StatusOs.REVISAO_ORCAMENTO) {
            os.setStatus(StatusOs.EM_AVALIACAO);
        }
        auditoria.registrar(ator, "ATRIBUIR", "ordem_servico", osId, "Mecânico designado: #" + funcionarioId);
        return os;
    }

    /** Compatibilidade com a API existente, que ainda aceita avaliação sem texto diagnóstico. */
    public Orcamento registrarAvaliacao(Long osId, List<ItemAvaliacao> avaliacao, UsuarioPrincipal ator) {
        return registrarAvaliacao(osId, null, avaliacao, ator);
    }

    /** Avaliação técnica com diagnóstico textual e itens, sem precificação/baixa de peças. */
    public Orcamento registrarAvaliacao(Long osId, String diagnostico,
                                        List<ItemAvaliacao> avaliacao, UsuarioPrincipal ator) {
        OrdemServico os = buscarOs(osId);
        validarMecanicoResponsavelOuGestao(os, ator);
        if (os.getStatus() != StatusOs.EM_AVALIACAO && os.getStatus() != StatusOs.REVISAO_ORCAMENTO) {
            throw new NegocioException("Esta OS não está aguardando avaliação ou revisão do orçamento.");
        }
        if (os.getFuncionario() == null) {
            throw new NegocioException("É necessário atribuir um mecânico antes da avaliação.");
        }
        if (diagnostico != null) {
            if (diagnostico.isBlank() || diagnostico.length() > 4000) {
                throw new NegocioException("Informe um diagnóstico técnico válido (até 4.000 caracteres).");
            }
            os.setDiagnostico(diagnostico.trim());
        }
        if (avaliacao == null || avaliacao.isEmpty() || avaliacao.size() > 100) {
            throw new NegocioException("Informe de 1 a 100 itens de serviço ou peça.");
        }
        int versao = orcamentos.findTopByOsIdOrderByVersaoDesc(osId)
                .map(o -> o.getVersao() + 1).orElse(1);
        Orcamento orcamento = new Orcamento();
        orcamento.setOs(os);
        orcamento.setVersao(versao);
        orcamento = orcamentos.save(orcamento);
        for (ItemAvaliacao solicitado : avaliacao) {
            if (solicitado == null || solicitado.tipo() == null || solicitado.descricao() == null
                    || solicitado.descricao().isBlank() || solicitado.descricao().length() > 255
                    || solicitado.quantidade() == null || solicitado.quantidade() <= 0
                    || solicitado.quantidade() > 100000) {
                throw new NegocioException("Item inválido: informe tipo, descrição e quantidade positiva.");
            }
            ItemOrcamento item = new ItemOrcamento();
            item.setOrcamento(orcamento);
            item.setTipo(solicitado.tipo());
            item.setDescricao(solicitado.descricao().trim());
            item.setQuantidade(solicitado.quantidade());
            if (solicitado.tipo() == TipoItemOrcamento.PECA) {
                if (solicitado.pecaId() == null) {
                    throw new NegocioException("Item do tipo PECA exige pecaId.");
                }
                Peca peca = pecas.findById(solicitado.pecaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada."));
                item.setPeca(peca);
            } else if (solicitado.pecaId() != null) {
                throw new NegocioException("Serviço não deve ter pecaId.");
            }
            itens.save(item);
        }
        os.setStatus(StatusOs.AGUARDANDO_PRECIFICACAO);
        auditoria.registrar(ator, "AVALIAR", "orcamento", orcamento.getId(),
                "Avaliação registrada, versão " + versao + " da OS #" + osId);
        return orcamento;
    }

    /** Somente o gestor informa os valores. Nenhum preco e definido pelo mecanico. */
    public Orcamento precificar(Long osId, Long orcamentoId, List<PrecoItem> precos, UsuarioPrincipal ator) {
        validarGestao(ator);
        OrdemServico os = buscarOs(osId);
        Orcamento orcamento = validarOrcamentoAtual(os, orcamentoId);
        if (os.getStatus() != StatusOs.AGUARDANDO_PRECIFICACAO) {
            throw new NegocioException("Orçamento não está aguardando precificação.");
        }
        List<ItemOrcamento> atuais = itens.findByOrcamentoIdOrderByIdAsc(orcamentoId);
        if (precos == null || precos.size() != atuais.size()) {
            throw new NegocioException("Informe o valor de todos os itens do orçamento.");
        }
        Map<Long, BigDecimal> valores = new HashMap<>();
        for (PrecoItem preco : precos) {
            if (preco == null || preco.itemId() == null || preco.valorUnitario() == null
                    || preco.valorUnitario().signum() < 0 || preco.valorUnitario().scale() > 2
                    || preco.valorUnitario().precision() - preco.valorUnitario().scale() > 8
                    || valores.putIfAbsent(preco.itemId(), preco.valorUnitario()) != null) {
                throw new NegocioException("Preço inválido ou item duplicado.");
            }
        }
        for (ItemOrcamento item : atuais) {
            BigDecimal valor = valores.get(item.getId());
            if (valor == null) {
                throw new NegocioException("Todos os itens devem receber preço.");
            }
            item.setValorUnitario(valor);
        }
        os.setStatus(StatusOs.AGUARDANDO_APROVACAO_GERENTE);
        auditoria.registrar(ator, "PRECIFICAR", "orcamento", orcamentoId, "Valores registrados pelo gerente");
        return orcamento;
    }

    public Orcamento aprovarGerente(Long osId, Long orcamentoId, LocalDate previsaoEntrega,
                                   UsuarioPrincipal ator) {
        validarGestao(ator);
        OrdemServico os = buscarOs(osId);
        Orcamento orcamento = validarOrcamentoAtual(os, orcamentoId);
        if (os.getStatus() != StatusOs.AGUARDANDO_APROVACAO_GERENTE) {
            throw new NegocioException("Orçamento não está aguardando aprovação do gerente.");
        }
        if (previsaoEntrega == null || previsaoEntrega.isBefore(LocalDate.now())) {
            throw new NegocioException("Informe uma previsão de entrega de hoje em diante.");
        }
        if (itens.findByOrcamentoIdOrderByIdAsc(orcamentoId).stream()
                .anyMatch(i -> i.getValorUnitario() == null)) {
            throw new NegocioException("Existem itens sem preço.");
        }
        // Prazo é definido ao aprovar o orçamento, e não na recepção do veículo.
        os.getVeiculo().setPrevisaoEntrega(previsaoEntrega.atTime(LocalTime.of(23, 59)));
        orcamento.setAprovadoGerente(true);
        os.setStatus(StatusOs.AGUARDANDO_CLIENTE);
        auditoria.registrar(ator, "APROVAR", "orcamento", orcamentoId,
                "Orçamento aprovado pelo gerente. Previsão de entrega: " + previsaoEntrega);
        return orcamento;
    }

    /** A assistente faz o contato; usuario que registrar a resposta pode ser outro. */
    public Orcamento registrarRespostaCliente(Long osId, Long orcamentoId, DecisaoCliente decisao,
                                               Long contatoPorUsuarioId, String observacao,
                                               UsuarioPrincipal ator) {
        OrdemServico os = buscarOs(osId);
        validarLeitura(os, ator);
        Orcamento orcamento = validarOrcamentoAtual(os, orcamentoId);
        if (os.getStatus() != StatusOs.AGUARDANDO_CLIENTE || !orcamento.isAprovadoGerente()
                || orcamento.getDecisaoCliente() != DecisaoCliente.PENDENTE) {
            throw new NegocioException("A resposta do cliente não pode ser registrada nesta etapa.");
        }
        if (decisao == null || decisao == DecisaoCliente.PENDENTE) {
            throw new NegocioException("Informe APROVADO, ALTERACAO ou RECUSADO.");
        }
        if (observacao != null && observacao.length() > 500) {
            throw new NegocioException("Observação limitada a 500 caracteres.");
        }
        if (contatoPorUsuarioId == null) {
            throw new NegocioException("Identifique a assistente que contatou o cliente.");
        }
        Usuario assistente = usuarios.findById(contatoPorUsuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário de contato não encontrado."));
        if (!assistente.isAtivo() || !Perfis.ASSISTENTE_ADMINISTRATIVO.equals(assistente.getFuncao().getNome())) {
            throw new NegocioException("O contato com o cliente deve ser registrado em nome da assistente.");
        }
        orcamento.setContatoPorUsuario(assistente);
        orcamento.setRegistradoPorUsuario(usuarios.getReferenceById(ator.getId()));
        orcamento.setDataContato(LocalDateTime.now());
        orcamento.setObservacaoCliente(observacao == null ? null : observacao.trim());
        orcamento.setDecisaoCliente(decisao);

        switch (decisao) {
            case ALTERACAO -> {
                // Orçamento alterado exige novo prazo na próxima aprovação da gestão.
                os.getVeiculo().setPrevisaoEntrega(null);
                os.setStatus(StatusOs.REVISAO_ORCAMENTO);
            }
            case RECUSADO -> {
                // Guarda RECUSADO na versao historica, mas permite negociar uma nova proposta.
                // A nova avaliacao cria uma NOVA versao, sem alterar a recusa anterior.
                os.getVeiculo().setPrevisaoEntrega(null);
                os.setStatus(StatusOs.REVISAO_ORCAMENTO);
            }
            case APROVADO -> {
                if (faltamPecas(orcamentoId)) {
                    os.setStatus(StatusOs.AGUARDANDO_PECAS);
                    notificarGestao(os);
                } else {
                    os.setStatus(StatusOs.AGUARDANDO_EXECUCAO);
                }
            }
            default -> throw new NegocioException("Decisão inválida.");
        }
        auditoria.registrar(ator, "RESPOSTA_CLIENTE", "orcamento", orcamentoId,
                "Resposta " + decisao + " da versão " + orcamento.getVersao() + " da OS #" + osId);
        return orcamento;
    }

    /** Recupera OS legadas travadas em ORCAMENTO_RECUSADO sem apagar a versao recusada. */
    public OrdemServico retomarOrcamentoRecusado(Long osId, UsuarioPrincipal ator) {
        validarGestao(ator);
        OrdemServico os = buscarOs(osId);
        if (os.getStatus() != StatusOs.ORCAMENTO_RECUSADO) {
            throw new NegocioException("Esta OS não possui recusa pendente de revisão.");
        }
        Orcamento ultimo = orcamentos.findTopByOsIdOrderByVersaoDesc(osId)
                .orElseThrow(() -> new NegocioException("Não existe orçamento para revisar."));
        if (ultimo.getDecisaoCliente() != DecisaoCliente.RECUSADO) {
            throw new NegocioException("A versão mais recente não está recusada.");
        }
        os.setStatus(StatusOs.REVISAO_ORCAMENTO);
        os.getVeiculo().setPrevisaoEntrega(null);
        auditoria.registrar(ator, "RETOMAR_REVISAO", "ordem_servico", osId,
                "Recusa mantida no orçamento #" + ultimo.getId() + "; nova avaliação liberada");
        return os;
    }

    /**
     * Admin Master pode voltar às etapas pré-execução. Uma versão já respondida
     * pelo cliente nunca é reprecificada nem reaberta: cria-se nova versão em REVISAO.
     */
    public OrdemServico adminRetornarEtapa(Long osId, EtapaCorrecao destino,
                                          String motivo, UsuarioPrincipal ator) {
        validarAdmin(ator);
        validarMotivo(motivo);
        OrdemServico os = buscarOs(osId);
        validarEtapaCorrigivel(os);
        if (destino == null) throw new NegocioException("Selecione a etapa para correção.");

        Orcamento ultimo = orcamentos.findTopByOsIdOrderByVersaoDesc(osId).orElse(null);
        boolean respondido = ultimo != null && ultimo.getDecisaoCliente() != DecisaoCliente.PENDENTE;
        StatusOs statusDestino;
        if (destino == EtapaCorrecao.ABERTURA || destino == EtapaCorrecao.AVALIACAO) {
            if (ultimo != null) {
                statusDestino = StatusOs.REVISAO_ORCAMENTO;
            } else if (destino == EtapaCorrecao.ABERTURA || os.getFuncionario() == null) {
                statusDestino = StatusOs.AGUARDANDO_AVALIACAO;
                if (destino == EtapaCorrecao.ABERTURA) os.setFuncionario(null);
            } else {
                statusDestino = StatusOs.EM_AVALIACAO;
            }
        } else {
            if (ultimo == null) throw new NegocioException("Primeiro registre a avaliação técnica.");
            if (respondido) {
                // Histórico imutável de resposta ao cliente, nova versão obrigatória.
                statusDestino = StatusOs.REVISAO_ORCAMENTO;
            } else if (destino == EtapaCorrecao.PRECIFICACAO) {
                ultimo.setAprovadoGerente(false);
                statusDestino = StatusOs.AGUARDANDO_PRECIFICACAO;
            } else if (destino == EtapaCorrecao.APROVACAO_GERENTE) {
                if (itens.findByOrcamentoIdOrderByIdAsc(ultimo.getId()).stream()
                        .anyMatch(item -> item.getValorUnitario() == null)) {
                    throw new NegocioException("Existem itens sem preço. Retorne primeiro à precificação.");
                }
                ultimo.setAprovadoGerente(false);
                statusDestino = StatusOs.AGUARDANDO_APROVACAO_GERENTE;
            } else {
                if (!ultimo.isAprovadoGerente()) {
                    throw new NegocioException("O orçamento precisa de aprovação interna antes do contato.");
                }
                if (os.getVeiculo().getPrevisaoEntrega() == null) {
                    throw new NegocioException("A previsão deve ser informada na aprovação do gerente.");
                }
                statusDestino = StatusOs.AGUARDANDO_CLIENTE;
            }
        }
        // Quando não retorna diretamente à espera de resposta, a previsão antiga deixa de valer.
        if (statusDestino != StatusOs.AGUARDANDO_CLIENTE) {
            os.getVeiculo().setPrevisaoEntrega(null);
        }
        StatusOs anterior = os.getStatus();
        os.setStatus(statusDestino);
        auditoria.registrar(ator, "CORRIGIR_ETAPA", "ordem_servico", osId,
                "De " + anterior + " para " + statusDestino + "; solicitação " + destino
                        + "; motivo: " + motivo.trim());
        return os;
    }

    /** Corrige a abertura. Trocar veículo após gerar orçamento apagaria o vínculo histórico. */
    public OrdemServico adminCorrigirAbertura(Long osId, Long clienteId, String placa,
                                              String problema, String motivo, UsuarioPrincipal ator) {
        validarAdmin(ator);
        validarMotivo(motivo);
        OrdemServico os = buscarOs(osId);
        validarEtapaCorrigivel(os);
        if (clienteId == null || placa == null || placa.isBlank() || problema == null
                || problema.isBlank() || problema.length() > 4000) {
            throw new NegocioException("Informe cliente, placa e descrição do problema (até 4.000 caracteres).");
        }
        String placaNormalizada = placa.trim().replace("-", "").replace(" ", "").toUpperCase(java.util.Locale.ROOT);
        Veiculo novo = veiculos.findByPlacaIgnoreCase(placaNormalizada)
                .orElseThrow(() -> new NegocioException("Placa não encontrada no cadastro."));
        if (!novo.getCliente().getId().equals(clienteId)) {
            throw new NegocioException("O veículo não pertence ao cliente escolhido.");
        }
        boolean mudouVeiculo = !novo.getId().equals(os.getVeiculo().getId());
        boolean mudouProblema = !problema.trim().equals(os.getProblema());
        if (!mudouVeiculo && !mudouProblema) {
            throw new NegocioException("Nenhuma informação foi alterada.");
        }
        Orcamento ultimo = orcamentos.findTopByOsIdOrderByVersaoDesc(osId).orElse(null);
        if (mudouVeiculo && ultimo != null) {
            throw new NegocioException("Não é possível trocar o veículo após criar orçamentos. "
                    + "Abra uma nova OS para o veículo correto e preserve o histórico desta.");
        }
        Long veiculoAnterior = os.getVeiculo().getId();
        if (mudouVeiculo) os.setVeiculo(novo);
        if (mudouProblema) os.setProblema(problema.trim());
        if (ultimo != null) {
            // Alterar problema depois de orçar exige novo orçamento, nunca edita o anterior.
            os.setStatus(StatusOs.REVISAO_ORCAMENTO);
            os.getVeiculo().setPrevisaoEntrega(null);
        }
        auditoria.registrar(ator, "CORRIGIR_ABERTURA", "ordem_servico", osId,
                "Veículo #" + veiculoAnterior + " -> #" + os.getVeiculo().getId()
                        + "; problema corrigido: " + mudouProblema + "; motivo: " + motivo.trim());
        return os;
    }

    /** Serviço(s) e peças aparecem na aprovação do cliente, mas NÃO consomem estoque até a execução. */
    @Transactional(readOnly = true)
    public List<ExecucaoServico> progressoServicos(Long osId, UsuarioPrincipal ator) {
        buscar(osId, ator);
        return execucoes.findByOsIdOrderById(osId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> quantidadesConsumidas(Long osId, UsuarioPrincipal ator) {
        buscar(osId, ator);
        return agruparConsumos(itensOs.findByOsIdOrderById(osId));
    }

    /** Após a compra/entrada das peças no módulo Estoque, só a gestão libera a OS. */
    public OrdemServico liberarPecas(Long osId, UsuarioPrincipal ator) {
        validarGestao(ator);
        OrdemServico os = bloquearOs(osId);
        if (os.getStatus() != StatusOs.AGUARDANDO_PECAS) {
            throw new NegocioException("Esta OS não está aguardando peças.");
        }
        Orcamento orcamento = orcamentoClienteAprovou(os);
        if (faltamPecas(orcamento.getId())) {
            throw new NegocioException("Ainda faltam peças no estoque. Registre a entrada antes de liberar a execução.");
        }
        os.setStatus(StatusOs.AGUARDANDO_EXECUCAO);
        auditoria.registrar(ator, "LIBERAR_PECAS", "ordem_servico", osId, "Peças disponíveis para a execução");
        return os;
    }

    /** O mecânico responsável inicia o reparo após aprovação do cliente e disponibilidade de estoque. */
    public OrdemServico iniciarExecucao(Long osId, UsuarioPrincipal ator) {
        OrdemServico os = bloquearOs(osId);
        validarMecanicoResponsavelOuGestao(os, ator);
        if (os.getStatus() != StatusOs.AGUARDANDO_EXECUCAO) {
            throw new NegocioException("Esta OS não está liberada para iniciar a execução.");
        }
        if (os.getFuncionario() == null) {
            throw new NegocioException("A OS precisa ter mecânico responsável.");
        }
        Orcamento aprovado = orcamentoClienteAprovou(os);
        if (faltamPecas(aprovado.getId())) {
            throw new NegocioException("O estoque mudou e já não é suficiente. Avise a gestão para providenciar as peças.");
        }
        for (ItemOrcamento item : itens.findByOrcamentoIdOrderByIdAsc(aprovado.getId())) {
            if (item.getTipo() == TipoItemOrcamento.SERVICO) {
                if (execucoes.findByOsIdAndItemOrcamentoId(osId, item.getId()).isEmpty()) {
                    ExecucaoServico execucao = new ExecucaoServico();
                    execucao.setOs(os);
                    execucao.setItemOrcamento(item);
                    execucoes.save(execucao);
                }
            }
        }
        os.setStatus(StatusOs.EM_ANDAMENTO);
        auditoria.registrar(ator, "INICIAR_EXECUCAO", "ordem_servico", osId, "Execução do orçamento #" + aprovado.getId());
        return os;
    }

    /** Atualiza cada serviço separadamente, sem permitir saltos de status ou edição do orçamento. */
    public ExecucaoServico atualizarServico(Long osId, Long itemId, StatusExecucaoServico proximo,
                                           UsuarioPrincipal ator) {
        OrdemServico os = bloquearOs(osId);
        validarMecanicoResponsavelOuGestao(os, ator);
        if (os.getStatus() != StatusOs.EM_ANDAMENTO) {
            throw new NegocioException("A OS não está em execução.");
        }
        Orcamento aprovado = orcamentoClienteAprovou(os);
        ItemOrcamento item = itens.findById(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));
        if (!item.getOrcamento().getId().equals(aprovado.getId())
                || item.getTipo() != TipoItemOrcamento.SERVICO) {
            throw new NegocioException("Somente serviços do orçamento aprovado podem ser executados.");
        }
        ExecucaoServico execucao = execucoes.findByOsIdAndItemOrcamentoId(osId, itemId)
                .orElseThrow(() -> new NegocioException("Serviço ainda não foi inicializado."));
        boolean valido = (execucao.getStatus() == StatusExecucaoServico.PENDENTE
                && proximo == StatusExecucaoServico.EM_ANDAMENTO)
                || (execucao.getStatus() == StatusExecucaoServico.EM_ANDAMENTO
                && proximo == StatusExecucaoServico.CONCLUIDO);
        if (!valido) throw new NegocioException("Mudança de status inválida para o serviço.");
        if (proximo == StatusExecucaoServico.EM_ANDAMENTO) execucao.setIniciadoEm(LocalDateTime.now());
        if (proximo == StatusExecucaoServico.CONCLUIDO) execucao.setConcluidoEm(LocalDateTime.now());
        execucao.setStatus(proximo);
        auditoria.registrar(ator, "EXECUTAR_SERVICO", "ordem_servico", osId,
                "Item #" + itemId + " avançou para " + proximo);
        return execucao;
    }

    /**
     * Registra somente as peças restantes do orçamento aprovado. INSERT em item_os
     * dispara o trigger existente de baixa de estoque; repetição não baixa duas vezes.
     */
    public OrdemServico registrarUsoPecas(Long osId, UsuarioPrincipal ator) {
        OrdemServico os = bloquearOs(osId);
        validarMecanicoResponsavelOuGestao(os, ator);
        if (os.getStatus() != StatusOs.EM_ANDAMENTO) {
            throw new NegocioException("Inicie a execução antes de registrar peças utilizadas.");
        }
        Orcamento aprovado = orcamentoClienteAprovou(os);
        Map<Long, Integer> necessarias = new HashMap<>();
        Map<Long, BigDecimal> precos = new HashMap<>();
        for (ItemOrcamento item : itens.findByOrcamentoIdOrderByIdAsc(aprovado.getId())) {
            if (item.getTipo() == TipoItemOrcamento.PECA) {
                Long pecaId = item.getPeca().getId();
                // Se a mesma peça constar em itens diferentes com preços diferentes,
                // a utilização exige consolidação comercial antes da execução.
                BigDecimal anterior = precos.putIfAbsent(pecaId, item.getValorUnitario());
                if (anterior != null && anterior.compareTo(item.getValorUnitario()) != 0) {
                    throw new NegocioException("Peça repetida com preços distintos. Solicite correção do orçamento à gestão.");
                }
                necessarias.merge(pecaId, item.getQuantidade(), Math::addExact);
            }
        }
        Map<Long, Integer> consumidas = agruparConsumos(itensOs.findByOsIdOrderById(osId));
        // Verifica todos os saldos ANTES de inserir qualquer consumo.
        for (Map.Entry<Long, Integer> entrada : necessarias.entrySet()) {
            int falta = entrada.getValue() - consumidas.getOrDefault(entrada.getKey(), 0);
            if (falta < 0) throw new NegocioException("A OS já consumiu mais peças do que o orçamento permite.");
            if (falta > 0) {
                Peca peca = pecas.findById(entrada.getKey())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada."));
                if (peca.getQuantidade() < falta) {
                    throw new NegocioException("Estoque insuficiente para " + peca.getNome() + ". Necessário: " + falta);
                }
            }
        }
        for (Map.Entry<Long, Integer> entrada : necessarias.entrySet()) {
            int falta = entrada.getValue() - consumidas.getOrDefault(entrada.getKey(), 0);
            if (falta > 0) {
                ItemOs registro = new ItemOs();
                registro.setOs(os);
                registro.setPeca(pecas.getReferenceById(entrada.getKey()));
                registro.setQuantidade(falta);
                registro.setValorUnitario(precos.get(entrada.getKey()));
                itensOs.saveAndFlush(registro); // executa o trigger de estoque nesta transação
            }
        }
        auditoria.registrar(ator, "CONSUMIR_PECAS", "ordem_servico", osId,
                "Utilização registrada conforme orçamento #" + aprovado.getId());
        return os;
    }

    /** Conclui tecnicamente somente após serviços finalizados e peças efetivamente consumidas. */
    public OrdemServico concluirExecucao(Long osId, UsuarioPrincipal ator) {
        OrdemServico os = bloquearOs(osId);
        validarMecanicoResponsavelOuGestao(os, ator);
        if (os.getStatus() != StatusOs.EM_ANDAMENTO) {
            throw new NegocioException("A OS não está em execução.");
        }
        Orcamento aprovado = orcamentoClienteAprovou(os);
        List<ItemOrcamento> aprovados = itens.findByOrcamentoIdOrderByIdAsc(aprovado.getId());
        List<ExecucaoServico> progresso = execucoes.findByOsIdOrderById(osId);
        long servicos = aprovados.stream().filter(i -> i.getTipo() == TipoItemOrcamento.SERVICO).count();
        if (progresso.size() != servicos || progresso.stream()
                .anyMatch(e -> e.getStatus() != StatusExecucaoServico.CONCLUIDO)) {
            throw new NegocioException("Conclua todos os serviços antes de finalizar a OS.");
        }
        Map<Long, Integer> necessarias = new HashMap<>();
        BigDecimal maoObra = BigDecimal.ZERO;
        for (ItemOrcamento item : aprovados) {
            if (item.getTipo() == TipoItemOrcamento.PECA) {
                necessarias.merge(item.getPeca().getId(), item.getQuantidade(), Math::addExact);
            } else {
                maoObra = maoObra.add(item.getValorUnitario()
                        .multiply(BigDecimal.valueOf(item.getQuantidade())));
            }
        }
        Map<Long, Integer> consumidas = agruparConsumos(itensOs.findByOsIdOrderById(osId));
        for (Map.Entry<Long, Integer> entrada : necessarias.entrySet()) {
            if (!entrada.getValue().equals(consumidas.getOrDefault(entrada.getKey(), 0))) {
                throw new NegocioException("Falta registrar a utilização das peças aprovadas.");
            }
        }
        if (consumidas.keySet().stream().anyMatch(id -> !necessarias.containsKey(id))) {
            throw new NegocioException("A OS possui peças consumidas fora do orçamento aprovado.");
        }
        os.setValorMaoObra(maoObra);
        os.setStatus(StatusOs.CONCLUIDA);
        auditoria.registrar(ator, "CONCLUIR_EXECUCAO", "ordem_servico", osId,
                "Execução concluída; mão de obra conforme orçamento #" + aprovado.getId());
        return os;
    }

    private OrdemServico bloquearOs(Long osId) {
        return ordens.bloquearParaExecucao(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
    }

    private Orcamento orcamentoClienteAprovou(OrdemServico os) {
        Orcamento aprovado = orcamentos.findTopByOsIdOrderByVersaoDesc(os.getId())
                .orElseThrow(() -> new NegocioException("OS sem orçamento."));
        if (!aprovado.isAprovadoGerente() || aprovado.getDecisaoCliente() != DecisaoCliente.APROVADO) {
            throw new NegocioException("A execução exige aprovação do gerente e do cliente na proposta atual.");
        }
        return aprovado;
    }

    private Map<Long, Integer> agruparConsumos(List<ItemOs> registros) {
        Map<Long, Integer> consumidas = new HashMap<>();
        for (ItemOs item : registros) {
            consumidas.merge(item.getPeca().getId(), item.getQuantidade(), Math::addExact);
        }
        return consumidas;
    }

    private void validarAdmin(UsuarioPrincipal ator) {
        validarAutenticacao(ator);
        if (!ator.isAdmin()) throw new AccessDeniedException("Ação exclusiva do Admin Master.");
    }

    private void validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.trim().length() > 350) {
            throw new NegocioException("Informe o motivo da correção (até 350 caracteres).");
        }
    }

    private void validarEtapaCorrigivel(OrdemServico os) {
        if (!ETAPAS_CORRIGIVEIS.contains(os.getStatus())) {
            throw new NegocioException("Esta OS já entrou em execução, pagamento ou encerramento. "
                    + "Não é seguro voltar etapas sem o fluxo de estorno correspondente.");
        }
    }

    private boolean faltamPecas(Long orcamentoId) {
        Map<Long, Integer> necessario = new HashMap<>();
        for (ItemOrcamento item : itens.findByOrcamentoIdOrderByIdAsc(orcamentoId)) {
            if (item.getTipo() == TipoItemOrcamento.PECA) {
                necessario.merge(item.getPeca().getId(), item.getQuantidade(), Math::addExact);
            }
        }
        for (Map.Entry<Long, Integer> entrada : necessario.entrySet()) {
            Peca peca = pecas.findById(entrada.getKey())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada."));
            if (peca.getQuantidade() < entrada.getValue()) return true;
        }
        return false;
    }

    private void notificarGestao(OrdemServico os) {
        for (Usuario usuario : usuarios.findAllByOrderByEmail()) {
            String perfil = usuario.getFuncao().getNome();
            if (usuario.isAtivo() && !usuario.isBloqueado()
                    && (Perfis.GERENTE.equals(perfil) || Perfis.ADMIN_MASTER.equals(perfil))) {
                Notificacao notificacao = new Notificacao();
                notificacao.setUsuario(usuario);
                notificacao.setTipo(TipoNotificacao.VEICULO_AGUARDANDO_PECA);
                notificacao.setMensagem("OS " + os.getNumero() + ": peças faltantes após aprovação do cliente.");
                notificacoes.save(notificacao);
            }
        }
    }

    private OrdemServico buscarOs(Long id) {
        return ordens.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
    }

    private Orcamento validarOrcamento(OrdemServico os, Long id) {
        Orcamento orcamento = orcamentos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento não encontrado."));
        if (!orcamento.getOs().getId().equals(os.getId())) {
            throw new NegocioException("Orçamento não pertence à OS informada.");
        }
        return orcamento;
    }

    private Orcamento validarOrcamentoAtual(OrdemServico os, Long id) {
        Orcamento orcamento = validarOrcamento(os, id);
        Orcamento atual = orcamentos.findTopByOsIdOrderByVersaoDesc(os.getId())
                .orElseThrow(() -> new NegocioException("OS sem orçamento."));
        if (!atual.getId().equals(orcamento.getId())) {
            throw new NegocioException("Somente a versão mais recente pode receber alterações.");
        }
        return orcamento;
    }

    private void validarAutenticacao(UsuarioPrincipal ator) {
        if (ator == null) throw new AccessDeniedException("Acesso não autenticado.");
    }

    private void validarRecepcaoOuGestao(UsuarioPrincipal ator) {
        validarAutenticacao(ator);
        if (!ator.isGerenteOuAdmin() && !ator.isAssistente())
            throw new AccessDeniedException("Somente recepção ou gestão podem abrir OS.");
    }

    private void validarGestao(UsuarioPrincipal ator) {
        validarAutenticacao(ator);
        if (!ator.isGerenteOuAdmin()) throw new AccessDeniedException("Ação exclusiva da gestão.");
    }

    private void validarMecanicoResponsavelOuGestao(OrdemServico os, UsuarioPrincipal ator) {
        validarAutenticacao(ator);
        if (ator.isGerenteOuAdmin()) return;
        if (!ator.isMecanico() || !ehMecanicoResponsavel(os, ator))
            throw new AccessDeniedException("Somente o mecânico responsável pode avaliar esta OS.");
    }

    private void validarLeitura(OrdemServico os, UsuarioPrincipal ator) {
        validarAutenticacao(ator);
        if (ator.isGerenteOuAdmin() || ator.isAssistente()) return;
        if (ator.isMecanico() && ehMecanicoResponsavel(os, ator)) return;
        throw new AccessDeniedException("A OS não está disponível para seu perfil.");
    }

    private boolean ehMecanicoResponsavel(OrdemServico os, UsuarioPrincipal ator) {
        return os.getFuncionario() != null && os.getFuncionario().getUsuario() != null
                && os.getFuncionario().getUsuario().getId().equals(ator.getId());
    }
}
