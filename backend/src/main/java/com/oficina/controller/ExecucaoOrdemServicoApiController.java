package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.model.ExecucaoServico;
import com.oficina.model.OrdemServico;
import com.oficina.model.StatusExecucaoServico;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.FluxoOrdemServicoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** API de execução: mesmos serviços e mesmas permissões do formulário Thymeleaf. */
@RestController
@RequestMapping("/api/os")
@RequiredArgsConstructor
public class ExecucaoOrdemServicoApiController {
    private final FluxoOrdemServicoService fluxo;

    public record Estado(Long id, String numero, String status) {
        static Estado de(OrdemServico os) { return new Estado(os.getId(), os.getNumero(), os.getStatus().name()); }
    }
    public record Progresso(Long itemId, String status, LocalDateTime iniciadoEm,
                           LocalDateTime concluidoEm) {
        static Progresso de(ExecucaoServico e) {
            return new Progresso(e.getItemOrcamento().getId(), e.getStatus().name(),
                    e.getIniciadoEm(), e.getConcluidoEm());
        }
    }
    public record AtualizarStatusRequest(@NotNull StatusExecucaoServico status) { }
    public record Erro(String mensagem) { }

    @GetMapping("/{id}/execucao")
    public List<Progresso> progresso(@PathVariable Long id,
                                     @AuthenticationPrincipal UsuarioPrincipal ator) {
        return fluxo.progressoServicos(id, ator).stream().map(Progresso::de).toList();
    }

    @GetMapping("/{id}/execucao/pecas-consumidas")
    public Map<Long, Integer> pecasConsumidas(@PathVariable Long id,
                                                @AuthenticationPrincipal UsuarioPrincipal ator) {
        return fluxo.quantidadesConsumidas(id, ator);
    }

    @PostMapping("/{id}/execucao/liberar-pecas")
    public Estado liberarPecas(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return Estado.de(fluxo.liberarPecas(id, ator));
    }

    @PostMapping("/{id}/execucao/iniciar")
    public Estado iniciar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return Estado.de(fluxo.iniciarExecucao(id, ator));
    }

    @PostMapping("/{id}/execucao/servicos/{itemId}")
    public Progresso atualizar(@PathVariable Long id, @PathVariable Long itemId,
                               @Valid @RequestBody AtualizarStatusRequest request,
                               @AuthenticationPrincipal UsuarioPrincipal ator) {
        return Progresso.de(fluxo.atualizarServico(id, itemId, request.status(), ator));
    }

    @PostMapping("/{id}/execucao/registrar-pecas")
    public Estado registrarPecas(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return Estado.de(fluxo.registrarUsoPecas(id, ator));
    }

    @PostMapping("/{id}/execucao/concluir")
    public Estado concluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator) {
        return Estado.de(fluxo.concluirExecucao(id, ator));
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Erro> negocio(NegocioException e) {
        return ResponseEntity.badRequest().body(new Erro(e.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Erro> naoEncontrado(RecursoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Erro(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Erro> semPermissao(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Erro("Acesso negado."));
    }
}
