package com.oficina.controller;

import com.oficina.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @Value("${app.security.lock-minutes:15}")
    private long minutosDeBloqueio;

    /** Quem já está logado não precisa ver a tela de login de novo. */
    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UsuarioPrincipal usuario, Model model) {
        if (usuario != null) {
            return "redirect:/";
        }
        model.addAttribute("minutosBloqueio", minutosDeBloqueio);
        return "login";
    }
}
