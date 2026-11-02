package com.oficina.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoNotificacao {
    ESTOQUE_BAIXO("Estoque baixo"),
    OS_ATRASADA("OS atrasada"),
    SOLICITACAO_PENDENTE("Solicitação pendente"),
    VEICULO_AGUARDANDO_PECA("Veículo aguardando peça"),
    SERVICO_CONCLUIDO("Serviço concluído");

    private final String rotulo;
}
