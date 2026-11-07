package com.oficina.exception;

/** Violação de uma regra de negócio. A mensagem é mostrada ao usuário, então nunca leva dados sensíveis. */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
