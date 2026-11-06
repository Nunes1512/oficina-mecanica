package com.oficina.controller;

import com.oficina.security.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /** Página provisória da Parte 2: confirma o login e o perfil. As próximas partes redirecionam por perfil. */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal UsuarioPrincipal usuario, Model model) {
        model.addAttribute("usuario", usuario);
        return "home";
    }
}
