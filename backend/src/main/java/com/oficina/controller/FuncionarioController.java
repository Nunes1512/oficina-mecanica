package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.form.FuncionarioForm;
import com.oficina.model.StatusFuncionario;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.FuncionarioService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE')")
public class FuncionarioController {

    private final FuncionarioService service;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("funcionarios", service.listar());
        return "funcionarios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new FuncionarioForm());
        preparar(model);
        return "funcionarios/form";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") FuncionarioForm form, BindingResult resultado,
                        @AuthenticationPrincipal UsuarioPrincipal usuario, Model model, RedirectAttributes redirect) {
        preparar(model);
        if (resultado.hasErrors()) {
            return "funcionarios/form";
        }
        try {
            service.criar(form, usuario);
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "funcionarios/form";
        }
        redirect.addFlashAttribute("sucesso", "Funcionário cadastrado com sucesso.");
        return "redirect:/funcionarios";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", FuncionarioForm.de(service.buscar(id)));
        model.addAttribute("id", id);
        preparar(model);
        return "funcionarios/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") FuncionarioForm form,
                            BindingResult resultado, @AuthenticationPrincipal UsuarioPrincipal usuario,
                            Model model, RedirectAttributes redirect) {
        model.addAttribute("id", id);
        preparar(model);
        if (resultado.hasErrors()) {
            return "funcionarios/form";
        }
        try {
            service.atualizar(id, form, usuario);
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "funcionarios/form";
        }
        redirect.addFlashAttribute("sucesso", "Funcionário atualizado com sucesso.");
        return "redirect:/funcionarios";
    }

    private void preparar(Model model) {
        model.addAttribute("statusList", StatusFuncionario.values());
    }
}
