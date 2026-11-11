package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.model.FormaPagamentoOs;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.FechamentoOsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Somente orquestra os formulários; permissões e transições ficam no service. */
@Controller
@RequestMapping("/ordens/{id}")
@RequiredArgsConstructor
public class FechamentoOsController {
    private final FechamentoOsService fechamento;

    @PostMapping("/liberar-pagamento")
    public String liberarPagamento(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator,
                                   RedirectAttributes redirect) {
        try {
            fechamento.liberarPagamento(id, ator);
            redirect.addFlashAttribute("sucesso", "Pagamento liberado para a recepção.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping(value = "/pagamento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String pagamento(@PathVariable Long id, @RequestParam FormaPagamentoOs forma,
                            @RequestParam(required = false) String observacao,
                            @RequestParam(required = false) MultipartFile comprovante,
                            @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            fechamento.registrarPagamento(id, forma, observacao, comprovante, ator);
            redirect.addFlashAttribute("sucesso", "Pagamento registrado. O veículo aguarda retirada.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @GetMapping("/pagamento/comprovante")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> comprovante(@PathVariable Long id,
                                                          @AuthenticationPrincipal UsuarioPrincipal ator) {
        FechamentoOsService.Comprovante arquivo = fechamento.baixarComprovante(id, ator);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.mime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(arquivo.dados().length)
                .body(new ByteArrayResource(arquivo.dados()));
    }

    @PostMapping("/retirada")
    public String retirada(@PathVariable Long id, @RequestParam String nomeRetirante,
                           @RequestParam(required = false) String observacao,
                           @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            fechamento.registrarRetirada(id, nomeRetirante, observacao, ator);
            redirect.addFlashAttribute("sucesso", "Retirada registrada. A OS aguarda encerramento pela gestão.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }

    @PostMapping("/encerrar")
    public String encerrar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal ator,
                           RedirectAttributes redirect) {
        try {
            fechamento.encerrar(id, ator);
            redirect.addFlashAttribute("sucesso", "Ordem de Serviço encerrada com sucesso.");
        } catch (NegocioException e) { redirect.addFlashAttribute("erro", e.getMessage()); }
        return "redirect:/ordens/" + id;
    }
}
