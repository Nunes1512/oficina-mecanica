package com.oficina.controller;

import com.oficina.exception.NegocioException;
import com.oficina.form.VeiculoForm;
import com.oficina.model.StatusVeiculo;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.service.ClienteService;
import com.oficina.service.VeiculoService;
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

/** Consultar é liberado a todo usuário logado. Cadastrar, alterar e excluir é só do gerente e do admin. */
@Controller
@RequestMapping("/veiculos")
@RequiredArgsConstructor
public class VeiculoController {

    private static final String GERENTE_OU_ADMIN = "hasAnyRole('ADMIN_MASTER','GERENTE')";

    private final VeiculoService service;
    private final ClienteService clienteService;

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("veiculos", service.listar(q));
        model.addAttribute("q", q);
        return "veiculos/lista";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("veiculo", service.buscar(id));
        model.addAttribute("timeline", service.timeline(id));
        return "veiculos/detalhe";
    }

    @GetMapping("/novo")
    @PreAuthorize(GERENTE_OU_ADMIN)
    public String novo(Model model) {
        model.addAttribute("form", new VeiculoForm());
        preparar(model);
        return "veiculos/form";
    }

    @PostMapping
    @PreAuthorize(GERENTE_OU_ADMIN)
    public String criar(@Valid @ModelAttribute("form") VeiculoForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        preparar(model);
        if (resultado.hasErrors()) {
            return "veiculos/form";
        }
        try {
            service.criar(form);
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "veiculos/form";
        }
        redirect.addFlashAttribute("sucesso", "Veículo cadastrado com sucesso.");
        return "redirect:/veiculos";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize(GERENTE_OU_ADMIN)
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", VeiculoForm.de(service.buscar(id)));
        model.addAttribute("id", id);
        preparar(model);
        return "veiculos/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize(GERENTE_OU_ADMIN)
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") VeiculoForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        model.addAttribute("id", id);
        preparar(model);
        if (resultado.hasErrors()) {
            return "veiculos/form";
        }
        try {
            service.atualizar(id, form);
        } catch (NegocioException erro) {
            resultado.reject("negocio", erro.getMessage());
            return "veiculos/form";
        }
        redirect.addFlashAttribute("sucesso", "Veículo atualizado com sucesso.");
        return "redirect:/veiculos/" + id;
    }

    @PostMapping("/{id}/excluir")
    @PreAuthorize(GERENTE_OU_ADMIN)
    public String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal usuario,
                          RedirectAttributes redirect) {
        try {
            service.excluir(id, usuario);
            redirect.addFlashAttribute("sucesso", "Veículo excluído.");
        } catch (NegocioException erro) {
            redirect.addFlashAttribute("erro", erro.getMessage());
        }
        return "redirect:/veiculos";
    }

    /** Dados dos campos de seleção do formulário. */
    private void preparar(Model model) {
        model.addAttribute("clientes", clienteService.listar(null));
        model.addAttribute("statusList", StatusVeiculo.values());
    }
}
