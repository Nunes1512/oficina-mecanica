package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.form.EntradaEstoqueForm;
import com.oficina.form.PecaForm;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.CatalogoPecasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pecas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE')")
public class PecaController {
    private final CatalogoPecasService service;

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("pecas", service.listar(q));
        model.addAttribute("busca", q == null ? "" : q);
        return "pecas/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("form", new PecaForm());
        return "pecas/form";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("form") PecaForm form, BindingResult errors,
                            @AuthenticationPrincipal UsuarioPrincipal ator,
                            RedirectAttributes redirect) {
        if (errors.hasErrors()) return "pecas/form";
        try {
            service.cadastrar(form, ator);
            redirect.addFlashAttribute("sucesso", "Peça cadastrada. Saldo inicial: 0. Registre uma entrada para adicionar estoque.");
            return "redirect:/pecas";
        } catch (NegocioException e) {
            errors.reject("negocio", e.getMessage());
            return "pecas/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", PecaForm.de(service.buscar(id)));
        model.addAttribute("id", id);
        return "pecas/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") PecaForm form,
                            BindingResult errors, @AuthenticationPrincipal UsuarioPrincipal ator,
                            Model model, RedirectAttributes redirect) {
        model.addAttribute("id", id);
        if (errors.hasErrors()) return "pecas/form";
        try {
            service.atualizar(id, form, ator);
            redirect.addFlashAttribute("sucesso", "Cadastro e preço de referência atualizados. Orçamentos anteriores preservados.");
            return "redirect:/pecas";
        } catch (NegocioException e) {
            errors.reject("negocio", e.getMessage());
            return "pecas/form";
        }
    }

    @GetMapping("/{id}/entrada")
    public String entrada(@PathVariable Long id, Model model) {
        model.addAttribute("peca", service.buscar(id));
        model.addAttribute("form", new EntradaEstoqueForm());
        return "pecas/entrada";
    }

    @PostMapping("/{id}/entrada")
    public String registrarEntrada(@PathVariable Long id,
                                   @Valid @ModelAttribute("form") EntradaEstoqueForm form,
                                   BindingResult errors, @AuthenticationPrincipal UsuarioPrincipal ator,
                                   Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("peca", service.buscar(id));
            return "pecas/entrada";
        }
        try {
            service.registrarEntrada(id, form, ator);
            redirect.addFlashAttribute("sucesso", "Entrada registrada no estoque com histórico de movimentação.");
            return "redirect:/pecas";
        } catch (NegocioException e) {
            errors.reject("negocio", e.getMessage());
            model.addAttribute("peca", service.buscar(id));
            return "pecas/entrada";
        }
    }
}
