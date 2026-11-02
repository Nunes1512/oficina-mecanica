package com.oficina.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusOs {
    ABERTA("Aberta"),
    EM_ANDAMENTO("Em andamento"),
    AGUARDANDO_PECAS("Aguardando peças"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada");

    private final String rotulo;
}
