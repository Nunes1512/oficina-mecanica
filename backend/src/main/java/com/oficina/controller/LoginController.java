package com.oficina.controller;

import com.oficina.security.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /** Quem já está logado não precisa ver a tela de login de novo. */
    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return usuario != null ? "redirect:/" : "login";
    }
}
