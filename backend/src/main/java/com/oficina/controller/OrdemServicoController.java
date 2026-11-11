package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.model.*;
import com.oficina.repository.*;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.FluxoOrdemServicoService;
import com.oficina.service.FechamentoOsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** UI de operação da OS. O serviço continua responsável por autorizações e transições. */
@Controller
@RequestMapping("/ordens")
@RequiredArgsConstructor
public class OrdemServicoController {
    private static final String RECEPCAO = "hasAnyRole('ADMIN_MASTER','GERENTE','ASSISTENTE_ADMINISTRATIVO')";

    private final FluxoOrdemServicoService fluxo;
    private final FechamentoOsService fechamento;
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final FuncionarioRepository funcionarios;
    private final PecaRepository pecas;

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioPrincipal ator, Model model) {
        model.addAttribute("ordens", fluxo.listar(ator));
        model.addAttribute("podeAbrir", ator.isGerenteOuAdmin() || ator.isAssistente());
        return "ordens/lista";
    }

    @GetMapping("/nova")
    @PreAuthorize(RECEPCAO)
    public String nova(Model model) {
        prepararAbertura(model);
        return "ordens/nova";
    }

    @PostMapping
    @PreAuthorize(RECEPCAO)
    public String abrir(@RequestParam Long clienteId, @RequestParam String placa,
                        @RequestParam String problema, @AuthenticationPrincipal UsuarioPrincipal ator,
                        Model model, RedirectAttributes redirect) {
        try {
            if (clienteId == null || !clientes.existsById(clienteId)) {
                throw new NegocioException("Selecione um cliente cadastrado.");
            }
            String placaNormalizada = placa == null ? "" : placa.trim().replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
            Veiculo veiculo = veiculos.findByPlacaIgnoreCase(placaNormalizada)
                    .orElseThrow(() -> new NegocioException("Placa não encontrada no cadastro de veículos."));
            if (!veiculo.getCliente().getId().equals(clienteId)) {
                throw new NegocioException("Este veículo pertence a outro cliente. Confira o proprietário cadastrado.");
            }
            OrdemServico os = fluxo.abrir(veiculo.getId(), problema, ator);
            redirect.addFlashAttribute("sucesso", "Ordem de Serviço " + os.getNumero() + " aberta com sucesso.");
            return "redirect:/ordens/" + os.getId();
        } catch (NegocioException | RecursoNaoEncontradoException erro) {
            model.addAttribute("erro", erro.getMessage());
            model.addAttribute("clienteSelecionado", clienteId);
            model.addAttribute("placaDigitada", placa);
            model.addAttribute("problemaDigitado", problema);
            prepararAbertura(model);
            return "ordens/nova";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator, Model model) {
        OrdemServico os = fluxo.buscar(id, ator);
        List<Orcamento> historico = fluxo.historicoOrcamentos(id, ator);
        java.util.Map<Long, String> nomesContatos = new java.util.HashMap<>();
        for (Orcamento versao : historico) {
            if (versao.getContatoPorUsuario() != null) {
                Long usuarioId = versao.getContatoPorUsuario().getId();
                funcionarios.findByUsuarioId(usuarioId)
                        .ifPresent(f -> nomesContatos.put(usuarioId, f.getNome()));
            }
        }
        Orcamento atual = historico.isEmpty() ? null : historico.get(0);
        List<ItemOrcamento> itens = atual == null ? List.of() : fluxo.listarItens(id, atual.getId(), ator);
        // O orçamento contém dois grupos distintos: mão de obra e peças reais do estoque.
        List<ItemOrcamento> servicos = itens.stream()
                .filter(i -> i.getTipo() == TipoItemOrcamento.SERVICO).toList();
        List<ItemOrcamento> pecasOrcadas = itens.stream()
                .filter(i -> i.getTipo() == TipoItemOrcamento.PECA).toList();
        BigDecimal totalServicos = BigDecimal.ZERO;
        BigDecimal totalPecas = BigDecimal.ZERO;
        for (ItemOrcamento item : itens) {
            if (item.getValorUnitario() != null) {
                BigDecimal subtotal = item.getValorUnitario()
                        .multiply(BigDecimal.valueOf(item.getQuantidade()));
                if (item.getTipo() == TipoItemOrcamento.SERVICO) totalServicos = totalServicos.add(subtotal);
                else totalPecas = totalPecas.add(subtotal);
            }
        }
        BigDecimal total = totalServicos.add(totalPecas);
        // REV é o número de recusas, não o ID ou a versão interna do orçamento.
        java.util.Map<Long, String> rotulosOrcamentos = new java.util.HashMap<>();
        int quantidadeRecusas = 0;
        for (int i = historico.size() - 1; i >= 0; i--) {
            Orcamento h = historico.get(i);
            if (h.getDecisaoCliente() == DecisaoCliente.RECUSADO) {
                quantidadeRecusas++;
                rotulosOrcamentos.put(h.getId(), "REV " + quantidadeRecusas);
            } else if (h.getDecisaoCliente() == DecisaoCliente.ALTERACAO) {
                rotulosOrcamentos.put(h.getId(), "SOLICITOU ALTERAÇÃO");
            } else if (h.getDecisaoCliente() == DecisaoCliente.APROVADO) {
                rotulosOrcamentos.put(h.getId(), "PROPOSTA APROVADA");
            } else {
                rotulosOrcamentos.put(h.getId(), quantidadeRecusas == 0 ? "PROPOSTA INICIAL" : "NOVA PROPOSTA");
            }
        }
        java.util.Map<Long, ExecucaoServico> progressoPorItem = new java.util.HashMap<>();
        java.util.Map<Long, Integer> consumidas = java.util.Map.of();
        if (java.util.Set.of(StatusOs.EM_ANDAMENTO, StatusOs.CONCLUIDA,
                StatusOs.AGUARDANDO_PAGAMENTO, StatusOs.AGUARDANDO_RETIRADA,
                StatusOs.VEICULO_RETIRADO, StatusOs.ENCERRADA).contains(os.getStatus())) {
            for (ExecucaoServico e : fluxo.progressoServicos(id, ator)) {
                progressoPorItem.put(e.getItemOrcamento().getId(), e);
            }
            consumidas = fluxo.quantidadesConsumidas(id, ator);
        }
        List<Funcionario> mecanicos = funcionarios.findByStatusOrderByNome(StatusFuncionario.ATIVO).stream()
                .filter(f -> f.getUsuario() != null && f.getUsuario().isAtivo() && !f.getUsuario().isBloqueado()
                        && Perfis.MECANICO.equals(f.getUsuario().getFuncao().getNome())).toList();
        // No formulário mostra o NOME do funcionário, mantendo o ID do usuário como vínculo de auditoria.
        List<Funcionario> assistentes = funcionarios.findByStatusOrderByNome(StatusFuncionario.ATIVO).stream()
                .filter(f -> f.getUsuario() != null && f.getUsuario().isAtivo()
                        && !f.getUsuario().isBloqueado()
                        && Perfis.ASSISTENTE_ADMINISTRATIVO.equals(f.getUsuario().getFuncao().getNome())).toList();
        boolean gestao = ator.isGerenteOuAdmin();
        boolean mecanicoResponsavel = ator.isMecanico() && os.getFuncionario() != null
                && os.getFuncionario().getUsuario() != null
                && os.getFuncionario().getUsuario().getId().equals(ator.getId());
        model.addAttribute("pagamento", (gestao || ator.isAssistente())
                ? fechamento.consultarPagamento(id, ator).orElse(null) : null);
        model.addAttribute("retirada", (gestao || ator.isAssistente())
                ? fechamento.consultarRetirada(id, ator).orElse(null) : null);
        model.addAttribute("formasPagamento", FormaPagamentoOs.values());
        model.addAttribute("podeRecepcao", ator.isAssistente() || ator.isGerenteOuAdmin());
        model.addAttribute("os", os);
        model.addAttribute("orcamento", atual);
        model.addAttribute("historico", historico);
        boolean osLegada = historico.isEmpty() && java.util.Set.of(
                StatusOs.ABERTA, StatusOs.EM_ANDAMENTO, StatusOs.AGUARDANDO_PECAS,
                StatusOs.CONCLUIDA).contains(os.getStatus());
        model.addAttribute("osLegada", osLegada);
        model.addAttribute("nomesContatos", nomesContatos);
        model.addAttribute("itens", itens);
        model.addAttribute("servicosOrcados", servicos);
        model.addAttribute("pecasOrcadas", pecasOrcadas);
        model.addAttribute("totalServicos", totalServicos);
        model.addAttribute("totalPecas", totalPecas);
        model.addAttribute("total", total);
        model.addAttribute("rotulosOrcamentos", rotulosOrcamentos);
        model.addAttribute("progressoPorItem", progressoPorItem);
        model.addAttribute("pecasConsumidas", consumidas);
        model.addAttribute("mecanicos", mecanicos);
        model.addAttribute("assistentes", assistentes);
        model.addAttribute("pecas", pecas.findAllByOrderByNome());
        model.addAttribute("gestao", gestao);
        model.addAttribute("admin", ator.isAdmin());
        model.addAttribute("podeCorrigirEtapas", ator.isAdmin() && java.util.Set.of(
                StatusOs.AGUARDANDO_AVALIACAO, StatusOs.EM_AVALIACAO,
                StatusOs.AGUARDANDO_PRECIFICACAO, StatusOs.AGUARDANDO_APROVACAO_GERENTE,
                StatusOs.AGUARDANDO_CLIENTE, StatusOs.REVISAO_ORCAMENTO,
                StatusOs.ORCAMENTO_RECUSADO, StatusOs.AGUARDANDO_PECAS,
                StatusOs.AGUARDANDO_EXECUCAO).contains(os.getStatus()));
        if (ator.isAdmin()) {
            model.addAttribute("clientes", clientes.findAllByOrderByNome());
            model.addAttribute("veiculos", veiculos.findAllByOrderByDataEntradaDesc());
        }
        model.addAttribute("podeAvaliar", gestao || mecanicoResponsavel);
        model.addAttribute("podeExecutar", gestao || mecanicoResponsavel);
        model.addAttribute("podeResponder", gestao || ator.isAssistente() || mecanicoResponsavel);
        return "ordens/detalhe";
    }

    @PostMapping("/{id}/mecanico")
    public String atribuir(@PathVariable Long id, @RequestParam Long funcionarioId,
                           @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            fluxo.atribuirMecanico(id, funcionarioId, ator);
            redirect.addFlashAttribute("sucesso", "Mecânico designado.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/avaliacao")
    public String avaliar(@PathVariable Long id, @RequestParam String diagnostico,
                          @RequestParam MultiValueMap<String, String> campos,
                          @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            // MultiValueMap preserva posições de valores vazios ("Não se aplica") em linhas dinâmicas.
            List<String> tipos = campos.getOrDefault("tipos", List.of());
            List<String> descricoes = campos.getOrDefault("descricoes", List.of());
            List<String> pecasIds = campos.getOrDefault("pecasIds", List.of());
            List<String> quantidades = campos.getOrDefault("quantidades", List.of());
            if (tipos.size() != descricoes.size() || tipos.size() != pecasIds.size()
                    || tipos.size() != quantidades.size()) {
                throw new NegocioException("Existem campos incompletos no formulário de avaliação.");
            }
            List<FluxoOrdemServicoService.ItemAvaliacao> avaliacao = new ArrayList<>();
            for (int i = 0; i < tipos.size(); i++) {
                String tipo = tipos.get(i).trim(), descricao = descricoes.get(i).trim(), peca = pecasIds.get(i).trim();
                if (tipo.isBlank() && descricao.isBlank() && peca.isBlank()) continue;
                if (tipo.isBlank() || descricao.isBlank() || quantidades.get(i).isBlank()) {
                    throw new NegocioException("Informe tipo, descrição e quantidade em cada linha preenchida.");
                }
                TipoItemOrcamento tipoItem = TipoItemOrcamento.valueOf(tipo);
                Long pecaId = peca.isBlank() ? null : Long.parseLong(peca);
                int quantidade = Integer.parseInt(quantidades.get(i));
                avaliacao.add(new FluxoOrdemServicoService.ItemAvaliacao(tipoItem, descricao, pecaId, quantidade));
            }
            if (diagnostico == null || diagnostico.isBlank() || diagnostico.length() > 4000) {
                throw new NegocioException("Informe o diagnóstico técnico (até 4.000 caracteres).");
            }
            if (avaliacao.isEmpty()) {
                throw new NegocioException("Adicione pelo menos um serviço ou peça antes de enviar a avaliação.");
            }
            fluxo.registrarAvaliacao(id, diagnostico, avaliacao, ator);
            redirect.addFlashAttribute("sucesso", "Avaliação registrada. Orçamento enviado para precificação.");
        } catch (NegocioException | IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e instanceof NegocioException ? e.getMessage() : "Confira os tipos, peças e quantidades informados.");
        }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/orcamentos/{orcamentoId}/precos")
    public String precificar(@PathVariable Long id, @PathVariable Long orcamentoId,
                             @RequestParam List<Long> itemIds, @RequestParam List<BigDecimal> valores,
                             @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            if (itemIds.size() != valores.size()) throw new NegocioException("Informe todos os preços.");
            List<FluxoOrdemServicoService.PrecoItem> lista = new ArrayList<>();
            for (int i = 0; i < itemIds.size(); i++) {
                lista.add(new FluxoOrdemServicoService.PrecoItem(itemIds.get(i), valores.get(i)));
            }
            fluxo.precificar(id, orcamentoId, lista, ator);
            redirect.addFlashAttribute("sucesso", "Preços informados. Orçamento aguarda aprovação interna.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/orcamentos/{orcamentoId}/aprovar-gerente")
    public String aprovar(@PathVariable Long id, @PathVariable Long orcamentoId,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate previsaoEntrega,
                          @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            fluxo.aprovarGerente(id, orcamentoId, previsaoEntrega, ator);
            redirect.addFlashAttribute("sucesso", "Orçamento aprovado internamente. Contate o cliente.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/orcamentos/{orcamentoId}/resposta-cliente")
    public String resposta(@PathVariable Long id, @PathVariable Long orcamentoId,
                           @RequestParam DecisaoCliente decisao, @RequestParam Long contatoPorUsuarioId,
                           @RequestParam(required = false) String observacao,
                           @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            fluxo.registrarRespostaCliente(id, orcamentoId, decisao, contatoPorUsuarioId, observacao, ator);
            redirect.addFlashAttribute("sucesso", "Resposta do cliente registrada.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/reabrir-recusa")
    public String reabrirRecusa(@PathVariable Long id,
                                @AuthenticationPrincipal UsuarioPrincipal ator,
                                RedirectAttributes redirect) {
        try {
            fluxo.retomarOrcamentoRecusado(id, ator);
            redirect.addFlashAttribute("sucesso", "OS liberada para nova avaliação. A recusa anterior foi preservada.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/corrigir-etapa")
    public String corrigirEtapa(@PathVariable Long id,
                               @RequestParam FluxoOrdemServicoService.EtapaCorrecao etapa,
                               @RequestParam String motivo,
                               @AuthenticationPrincipal UsuarioPrincipal ator,
                               RedirectAttributes redirect) {
        try {
            OrdemServico os = fluxo.adminRetornarEtapa(id, etapa, motivo, ator);
            redirect.addFlashAttribute("sucesso", "Etapa corrigida para: " + os.getStatus().getRotulo()
                    + ". Orçamentos já respondidos permanecem no histórico.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/corrigir-abertura")
    public String corrigirAbertura(@PathVariable Long id, @RequestParam Long clienteId,
                                  @RequestParam String placa, @RequestParam String problema,
                                  @RequestParam String motivo,
                                  @AuthenticationPrincipal UsuarioPrincipal ator,
                                  RedirectAttributes redirect) {
        try {
            fluxo.adminCorrigirAbertura(id, clienteId, placa, problema, motivo, ator);
            redirect.addFlashAttribute("sucesso", "Dados corrigidos. Caso exista orçamento, uma nova versão deve ser avaliada.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/liberar-pecas")
    public String liberarPecas(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator,
                               RedirectAttributes redirect) {
        try {
            fluxo.liberarPecas(id, ator);
            redirect.addFlashAttribute("sucesso", "Peças disponíveis. A OS foi liberada para execução.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/iniciar-execucao")
    public String iniciarExecucao(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator,
                                  RedirectAttributes redirect) {
        try {
            fluxo.iniciarExecucao(id, ator);
            redirect.addFlashAttribute("sucesso", "Execução iniciada. Registre o andamento de cada serviço.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/servicos/{itemId}/status")
    public String atualizarServico(@PathVariable Long id, @PathVariable Long itemId,
                                    @RequestParam StatusExecucaoServico status,
                                    @AuthenticationPrincipal UsuarioPrincipal ator,
                                    RedirectAttributes redirect) {
        try {
            fluxo.atualizarServico(id, itemId, status, ator);
            redirect.addFlashAttribute("sucesso", "Andamento do serviço atualizado.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/registrar-uso-pecas")
    public String usarPecas(@PathVariable Long id, @RequestParam List<Long> pecaIds,
                             @RequestParam List<Integer> quantidades,
                             @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            if (pecaIds.size() != quantidades.size()) {
                throw new NegocioException("Informe a quantidade de cada peça selecionada.");
            }
            java.util.Map<Long, Integer> porPeca = new java.util.HashMap<>();
            for (int i = 0; i < pecaIds.size(); i++) {
                Integer quantidade = quantidades.get(i);
                if (pecaIds.get(i) == null || quantidade == null || quantidade < 0)
                    throw new NegocioException("Quantidade de peça inválida.");
                porPeca.merge(pecaIds.get(i), quantidade, Math::addExact);
            }
            fluxo.registrarUsoPecas(id, porPeca, ator);
            redirect.addFlashAttribute("sucesso", "Utilização de peças registrada no estoque.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/{id}/concluir-execucao")
    public String concluirExecucao(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator,
                                    RedirectAttributes redirect) {
        try {
            fluxo.concluirExecucao(id, ator);
            redirect.addFlashAttribute("sucesso", "Execução concluída. A gestão deve liberar o pagamento.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    private void prepararAbertura(Model model) {
        model.addAttribute("clientes", clientes.findAllByOrderByNome());
        model.addAttribute("veiculos", veiculos.findAllByOrderByDataEntradaDesc());
    }
}
