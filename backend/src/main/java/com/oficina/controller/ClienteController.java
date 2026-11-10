package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.form.ClienteForm;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.ClienteService;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE')")
public class ClienteController {

    private final ClienteService service;

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("clientes", service.listar(q));
        model.addAttribute("q", q);
        // O indicador de cadastros mensais fica reservado ao futuro dashboard gerencial.
        return "clientes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new ClienteForm());
        return "clientes/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") ClienteForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "clientes/form";
        }
        try {
            service.criar(form);
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "clientes/form";
        }
        redirect.addFlashAttribute("sucesso", "Cliente cadastrado com sucesso.");
        return "redirect:/clientes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", ClienteForm.de(service.buscar(id)));
        model.addAttribute("id", id);
        return "clientes/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ClienteForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        model.addAttribute("id", id);
        if (resultado.hasErrors()) {
            return "clientes/form";
        }
        try {
            service.atualizar(id, form);
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "clientes/form";
        }
        redirect.addFlashAttribute("sucesso", "Cliente atualizado com sucesso.");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal usuario,
                          RedirectAttributes redirect) {
        try {
            service.excluir(id, usuario);
            redirect.addFlashAttribute("sucesso", "Cliente excluído.");
        } catch (NegocioException erro) {
            redirect.addFlashAttribute("erro", erro.getMessage());
        }
        return "redirect:/clientes";
    }
}
