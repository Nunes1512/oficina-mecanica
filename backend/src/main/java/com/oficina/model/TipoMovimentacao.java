package com.oficina.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoMovimentacao {
    ENTRADA("Entrada"),
    SAIDA("Saída");

    private final String rotulo;
}
