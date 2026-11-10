package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.model.DecisaoCliente;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    public record ItemAvaliacao(TipoItemOrcamento tipo, String descricao, Long pecaId, Integer quantidade) { }
    public record PrecoItem(Long itemId, BigDecimal valorUnitario) { }

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
        os.setDataAbertura(LocalDateTime.now());
        os = ordens.save(os);
        auditoria.registrar(ator, "CRIAR", "ordem_servico", os.getId(), "OS aberta para avaliação");
        return os;
    }

    public OrdemServico atribuirMecanico(Long osId, Long funcionarioId, UsuarioPrincipal ator) {
        validarGestao(ator);
        OrdemServico os = buscarOs(osId);
        if (os.getStatus() != StatusOs.AGUARDANDO_AVALIACAO && os.getStatus() != StatusOs.EM_AVALIACAO) {
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
        os.setStatus(StatusOs.EM_AVALIACAO);
        auditoria.registrar(ator, "ATRIBUIR", "ordem_servico", osId, "Mecânico designado: #" + funcionarioId);
        return os;
    }

    /** Avaliacao original ou nova versao, sempre com itens sem precos. */
    public Orcamento registrarAvaliacao(Long osId, List<ItemAvaliacao> avaliacao, UsuarioPrincipal ator) {
        OrdemServico os = buscarOs(osId);
        validarMecanicoResponsavelOuGestao(os, ator);
        if (os.getStatus() != StatusOs.EM_AVALIACAO && os.getStatus() != StatusOs.REVISAO_ORCAMENTO) {
            throw new NegocioException("Esta OS não está aguardando avaliação ou revisão do orçamento.");
        }
        if (os.getFuncionario() == null) {
            throw new NegocioException("É necessário atribuir um mecânico antes da avaliação.");
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

    public Orcamento aprovarGerente(Long osId, Long orcamentoId, UsuarioPrincipal ator) {
        validarGestao(ator);
        OrdemServico os = buscarOs(osId);
        Orcamento orcamento = validarOrcamentoAtual(os, orcamentoId);
        if (os.getStatus() != StatusOs.AGUARDANDO_APROVACAO_GERENTE) {
            throw new NegocioException("Orçamento não está aguardando aprovação do gerente.");
        }
        if (itens.findByOrcamentoIdOrderByIdAsc(orcamentoId).stream()
                .anyMatch(i -> i.getValorUnitario() == null)) {
            throw new NegocioException("Existem itens sem preço.");
        }
        orcamento.setAprovadoGerente(true);
        os.setStatus(StatusOs.AGUARDANDO_CLIENTE);
        auditoria.registrar(ator, "APROVAR", "orcamento", orcamentoId, "Orçamento aprovado pelo gerente");
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
            case ALTERACAO -> os.setStatus(StatusOs.REVISAO_ORCAMENTO);
            case RECUSADO -> os.setStatus(StatusOs.ORCAMENTO_RECUSADO);
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
