package com.oficina.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventoTimeline {
    CADASTRO("Cadastro do veículo"),
    ENTRADA("Entrada na oficina"),
    DIAGNOSTICO("Diagnóstico"),
    SOLICITACAO_PECA("Solicitação de peça"),
    APROVACAO_PECA("Aprovação de peça"),
    INICIO_CONSERTO("Início do conserto"),
    MUDANCA_STATUS("Mudança de status"),
    CONCLUSAO("Conclusão"),
    ENTREGA("Entrega ao cliente");
    private final String rotulo;
}
