package com.oficina.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusFuncionario {
    ATIVO("Ativo"),
    INATIVO("Inativo");

    private final String rotulo;
}
