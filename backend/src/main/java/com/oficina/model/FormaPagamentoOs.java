package com.oficina.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormaPagamentoOs {
    DINHEIRO("Dinheiro"), PIX("Pix"), CARTAO_DEBITO("Cartão de débito"),
    CARTAO_CREDITO("Cartão de crédito"), TRANSFERENCIA("Transferência");

    private final String rotulo;
}
