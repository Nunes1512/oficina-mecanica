package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.GerenciamentoUsuariosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/minha-conta")
@RequiredArgsConstructor
public class MinhaContaController {

    private final GerenciamentoUsuariosService service;

    @GetMapping("/senha")
    public String form(Model model) {
        return "conta/senha";
    }

    @PostMapping("/senha")
    public String alterar(@RequestParam String senhaAtual, @RequestParam String novaSenha,
                          @RequestParam String confirmacao,
                          @AuthenticationPrincipal UsuarioPrincipal ator,
                          Authentication autenticacao,
                          HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            service.trocarMinhaSenha(senhaAtual, novaSenha, confirmacao, ator);
            new SecurityContextLogoutHandler().logout(request, response, autenticacao);
            return "redirect:/login?senhaAlterada";
        } catch (NegocioException erro) {
            model.addAttribute("erro", erro.getMessage());
            return "conta/senha";
        }
    }
}
