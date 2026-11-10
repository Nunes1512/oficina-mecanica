package com.oficina.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Compatibilidade com links antigos: toda administração de acesso agora ocorre em Funcionários. */
@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN_MASTER')")
public class GerenciamentoUsuariosController {
    @GetMapping({"", "/{id}/editar"})
    public String redirecionar() { return "redirect:/funcionarios"; }
}
