package com.oficina.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusVeiculo {
    AGUARDANDO_ANALISE("Aguardando análise"),
    AGUARDANDO_PECAS("Aguardando peças"),
    EM_MANUTENCAO("Em manutenção"),
    TESTES_FINAIS("Testes finais"),
    CONCLUIDO("Concluído"),
    ENTREGUE("Entregue");

    private final String rotulo;
}
