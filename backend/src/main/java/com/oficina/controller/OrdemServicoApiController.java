package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.model.DecisaoCliente;
import com.oficina.model.ItemOrcamento;
import com.oficina.model.Orcamento;
import com.oficina.model.OrdemServico;
import com.oficina.model.StatusOs;
import com.oficina.model.TipoItemOrcamento;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.FluxoOrdemServicoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** API interna para integrar as telas Thymeleaf da Parte 4. Mantem CSRF e sessao ativos. */
@RestController
@RequestMapping("/api/os")
@RequiredArgsConstructor
public class OrdemServicoApiController {
    private final FluxoOrdemServicoService fluxo;

    public record AbrirRequest(@NotNull Long veiculoId, @NotBlank String problema) { }
    public record AtribuirRequest(@NotNull Long funcionarioId) { }
    public record AvaliacaoRequest(@NotNull List<FluxoOrdemServicoService.ItemAvaliacao> itens) { }
    public record PrecosRequest(@NotNull List<FluxoOrdemServicoService.PrecoItem> itens) { }
    public record RespostaRequest(@NotNull DecisaoCliente decisao, @NotNull Long contatoPorUsuarioId,
                                  String observacao) { }

    public record OsResposta(Long id, String numero, Long veiculoId, Long mecanicoId, StatusOs status) {
        static OsResposta de(OrdemServico os) {
            return new OsResposta(os.getId(), os.getNumero(), os.getVeiculo().getId(),
                    os.getFuncionario() == null ? null : os.getFuncionario().getId(), os.getStatus());
        }
    }
    public record OrcamentoResposta(Long id, Long osId, Integer versao, boolean aprovadoGerente,
                                    DecisaoCliente decisaoCliente, LocalDateTime dataContato) {
        static OrcamentoResposta de(Orcamento o) {
            return new OrcamentoResposta(o.getId(), o.getOs().getId(), o.getVersao(),
                    o.isAprovadoGerente(), o.getDecisaoCliente(), o.getDataContato());
        }
    }
    public record ItemResposta(Long id, TipoItemOrcamento tipo, String descricao, Long pecaId,
                               Integer quantidade, BigDecimal valorUnitario) {
        static ItemResposta de(ItemOrcamento i) {
            return new ItemResposta(i.getId(), i.getTipo(), i.getDescricao(),
                    i.getPeca() == null ? null : i.getPeca().getId(),
                    i.getQuantidade(), i.getValorUnitario());
        }
    }

    @GetMapping
    public List<OsResposta> listar(@AuthenticationPrincipal UsuarioPrincipal ator) {
        return fluxo.listar(ator).stream().map(OsResposta::de).toList();
    }

    @GetMapping("/{id}")
    public OsResposta buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return OsResposta.de(fluxo.buscar(id, ator));
    }

    @GetMapping("/{id}/orcamentos")
    public List<OrcamentoResposta> historico(@PathVariable Long id,
                                              @AuthenticationPrincipal UsuarioPrincipal ator) {
        return fluxo.historicoOrcamentos(id, ator).stream().map(OrcamentoResposta::de).toList();
    }

    @GetMapping("/{id}/orcamentos/{orcamentoId}/itens")
    public List<ItemResposta> itens(@PathVariable Long id, @PathVariable Long orcamentoId,
                                    @AuthenticationPrincipal UsuarioPrincipal ator) {
        return fluxo.listarItens(id, orcamentoId, ator).stream().map(ItemResposta::de).toList();
    }

    @PostMapping
    public ResponseEntity<OsResposta> abrir(@Valid @RequestBody AbrirRequest request,
                                           @AuthenticationPrincipal UsuarioPrincipal ator) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OsResposta.de(fluxo.abrir(request.veiculoId(), request.problema(), ator)));
    }

    @PostMapping("/{id}/mecanico")
    public OsResposta atribuir(@PathVariable Long id, @Valid @RequestBody AtribuirRequest request,
                               @AuthenticationPrincipal UsuarioPrincipal ator) {
        return OsResposta.de(fluxo.atribuirMecanico(id, request.funcionarioId(), ator));
    }

    @PostMapping("/{id}/avaliacao")
    public ResponseEntity<OrcamentoResposta> avaliar(@PathVariable Long id,
                                                    @Valid @RequestBody AvaliacaoRequest request,
                                                    @AuthenticationPrincipal UsuarioPrincipal ator) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrcamentoResposta.de(fluxo.registrarAvaliacao(id, request.itens(), ator)));
    }

    @PostMapping("/{id}/orcamentos/{orcamentoId}/precos")
    public OrcamentoResposta precificar(@PathVariable Long id, @PathVariable Long orcamentoId,
                                       @Valid @RequestBody PrecosRequest request,
                                       @AuthenticationPrincipal UsuarioPrincipal ator) {
        return OrcamentoResposta.de(fluxo.precificar(id, orcamentoId, request.itens(), ator));
    }

    @PostMapping("/{id}/orcamentos/{orcamentoId}/aprovar-gerente")
    public OrcamentoResposta aprovarGerente(@PathVariable Long id, @PathVariable Long orcamentoId,
                                           @AuthenticationPrincipal UsuarioPrincipal ator) {
        return OrcamentoResposta.de(fluxo.aprovarGerente(id, orcamentoId, ator));
    }

    @PostMapping("/{id}/orcamentos/{orcamentoId}/resposta-cliente")
    public OrcamentoResposta registrarResposta(@PathVariable Long id, @PathVariable Long orcamentoId,
                                               @Valid @RequestBody RespostaRequest request,
                                               @AuthenticationPrincipal UsuarioPrincipal ator) {
        return OrcamentoResposta.de(fluxo.registrarRespostaCliente(id, orcamentoId, request.decisao(),
                request.contatoPorUsuarioId(), request.observacao(), ator));
    }

    @RestControllerAdvice(assignableTypes = OrdemServicoApiController.class)
    public static class ErrosApi {
        public record Erro(String mensagem) { }
        @ExceptionHandler(NegocioException.class)
        public ResponseEntity<Erro> negocio(NegocioException e) {
            return ResponseEntity.badRequest().body(new Erro(e.getMessage()));
        }
        @ExceptionHandler(RecursoNaoEncontradoException.class)
        public ResponseEntity<Erro> naoEncontrado(RecursoNaoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Erro(e.getMessage()));
        }
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Erro> proibido(AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Erro("Acesso negado."));
        }
    }
}
