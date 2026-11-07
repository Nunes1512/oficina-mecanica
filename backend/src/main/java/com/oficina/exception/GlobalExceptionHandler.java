package com.oficina.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Trata só os erros esperados do sistema. Não existe um handler genérico de Exception de propósito:
 * ele engoliria o AccessDeniedException e trocaria o "acesso negado" (403) por um erro 500.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado() {
        return "error/404";
    }

    @ExceptionHandler(NegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String regraDeNegocio(NegocioException erro, Model model) {
        model.addAttribute("mensagem", erro.getMessage());
        return "error/negocio";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String integridade(Model model) {
        model.addAttribute("mensagem",
                "A operação não pôde ser concluída: o registro está em uso por outro cadastro ou os dados são inválidos.");
        return "error/negocio";
    }
}
