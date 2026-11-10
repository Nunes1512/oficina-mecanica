package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.form.FuncionarioForm;
import com.oficina.model.Funcionario;
import com.oficina.model.StatusFuncionario;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.FuncionarioService;
import com.oficina.service.GerenciamentoUsuariosService;
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

/** Funcionários e acesso ao sistema pertencem ao mesmo módulo. */
@Controller
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE')")
public class FuncionarioController {
    private final FuncionarioService service;
    private final GerenciamentoUsuariosService acessos;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("funcionarios", service.listar());
        return "funcionarios/lista";
    }

    @GetMapping("/novo")
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public String novo(Model model) {
        model.addAttribute("form", new FuncionarioForm());
        model.addAttribute("id", null);
        model.addAttribute("temUsuario", false);
        preparar(model);
        return "funcionarios/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public String criar(@Valid @ModelAttribute("form") FuncionarioForm form, BindingResult resultado,
                        @AuthenticationPrincipal UsuarioPrincipal ator, Model model, RedirectAttributes redirect) {
        model.addAttribute("temUsuario", false);
        preparar(model);
        if (resultado.hasErrors()) return "funcionarios/form";
        try {
            Funcionario criado = service.criar(form, ator);
            redirect.addFlashAttribute("sucesso", "Funcionário e acesso criados: " + criado.getUsuario().getEmail());
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "funcionarios/form";
        }
        return "redirect:/funcionarios";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Funcionario funcionario = service.buscar(id);
        model.addAttribute("form", FuncionarioForm.de(funcionario));
        model.addAttribute("id", id);
        model.addAttribute("temUsuario", funcionario.getUsuario() != null);
        model.addAttribute("conta", funcionario.getUsuario());
        preparar(model);
        return "funcionarios/form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") FuncionarioForm form,
                            BindingResult resultado, @AuthenticationPrincipal UsuarioPrincipal ator,
                            Model model, RedirectAttributes redirect) {
        Funcionario funcionario = service.buscar(id);
        model.addAttribute("id", id);
        model.addAttribute("temUsuario", funcionario.getUsuario() != null);
        model.addAttribute("conta", funcionario.getUsuario());
        preparar(model);
        if (resultado.hasErrors()) return "funcionarios/form";
        try {
            service.atualizar(id, form, ator);
            redirect.addFlashAttribute("sucesso", "Cadastro atualizado.");
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "funcionarios/form";
        }
        return "redirect:/funcionarios/" + id + "/editar";
    }

    @PostMapping("/{id}/perfil")
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public String alterarPerfil(@PathVariable Long id, @RequestParam String perfil,
                                @AuthenticationPrincipal UsuarioPrincipal ator, RedirectAttributes redirect) {
        try {
            Long usuarioId = idUsuario(id);
            acessos.alterarPerfil(usuarioId, perfil, ator);
            redirect.addFlashAttribute("sucesso", "Perfil atualizado. Sessões anteriores foram encerradas.");
        } catch (NegocioException erro) { redirect.addFlashAttribute("erro", erro.getMessage()); }
        return "redirect:/funcionarios/" + id + "/editar";
    }

    @PostMapping("/{id}/senha")
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public String redefinirSenha(@PathVariable Long id, @RequestParam String novaSenha,
                                 @RequestParam String confirmacao, @AuthenticationPrincipal UsuarioPrincipal ator,
                                 RedirectAttributes redirect) {
        try {
            Long usuarioId = idUsuario(id);
            acessos.redefinirSenha(usuarioId, novaSenha, confirmacao, ator);
            redirect.addFlashAttribute("sucesso", "Senha redefinida. O funcionário deverá fazer login novamente.");
        } catch (NegocioException erro) { redirect.addFlashAttribute("erro", erro.getMessage()); }
        return "redirect:/funcionarios/" + id + "/editar";
    }

    private Long idUsuario(Long funcionarioId) {
        Funcionario funcionario = service.buscar(funcionarioId);
        if (funcionario.getUsuario() == null) {
            throw new NegocioException("Este funcionário ainda não tem login vinculado.");
        }
        return funcionario.getUsuario().getId();
    }

    private void preparar(Model model) {
        model.addAttribute("statusList", StatusFuncionario.values());
        model.addAttribute("perfisUsuario", new String[]{Perfis.MECANICO, Perfis.ASSISTENTE_ADMINISTRATIVO, Perfis.GERENTE});
    }
}
