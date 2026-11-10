package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.form.FuncionarioForm;
import com.oficina.model.StatusFuncionario;
import com.oficina.security.Perfis;
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
        model.addAttribute("temUsuario", false);
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
            var criado = service.criar(form, usuario);
            redirect.addFlashAttribute("sucesso", criado.getUsuario() == null
                    ? "Funcionário cadastrado SEM login de acesso."
                    : "Funcionário e login criados com sucesso: " + criado.getUsuario().getEmail());
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "funcionarios/form";
        }
        return "redirect:/funcionarios";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        var funcionario = service.buscar(id);
        model.addAttribute("form", FuncionarioForm.de(funcionario));
        model.addAttribute("temUsuario", funcionario.getUsuario() != null);
        model.addAttribute("id", id);
        preparar(model);
        return "funcionarios/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") FuncionarioForm form,
                            BindingResult resultado, @AuthenticationPrincipal UsuarioPrincipal usuario,
                            Model model, RedirectAttributes redirect) {
        model.addAttribute("id", id);
        model.addAttribute("temUsuario", service.buscar(id).getUsuario() != null);
        preparar(model);
        if (resultado.hasErrors()) {
            return "funcionarios/form";
        }
        try {
            var atualizado = service.atualizar(id, form, usuario);
            redirect.addFlashAttribute("sucesso", atualizado.getUsuario() == null
                    ? "Funcionário atualizado SEM login de acesso."
                    : "Funcionário atualizado. Login vinculado: " + atualizado.getUsuario().getEmail());
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "funcionarios/form";
        }
        return "redirect:/funcionarios";
    }

    private void preparar(Model model) {
        model.addAttribute("statusList", StatusFuncionario.values());
        model.addAttribute("perfisUsuario", new String[] {Perfis.FUNCIONARIO, Perfis.GERENTE});
    }
}
