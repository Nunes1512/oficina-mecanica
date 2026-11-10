package com.oficina.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusOs {
    // Estados legados mantidos para leitura das OS anteriores a V10.
    ABERTA("Aberta (legado)"),
    AGUARDANDO_AVALIACAO("Aguardando avaliação"),
    EM_AVALIACAO("Em avaliação"),
    AGUARDANDO_PRECIFICACAO("Aguardando precificação"),
    AGUARDANDO_APROVACAO_GERENTE("Aguardando aprovação do gerente"),
    AGUARDANDO_CLIENTE("Aguardando aprovação do cliente"),
    REVISAO_ORCAMENTO("Orçamento em revisão"),
    ORCAMENTO_RECUSADO("Orçamento recusado"),
    AGUARDANDO_PECAS("Aguardando peças"),
    AGUARDANDO_EXECUCAO("Aguardando execução"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída"),
    AGUARDANDO_PAGAMENTO("Aguardando pagamento"),
    AGUARDANDO_RETIRADA("Aguardando retirada"),
    VEICULO_RETIRADO("Veículo retirado"),
    ENCERRADA("Encerrada"),
    CANCELADA("Cancelada");

    private final String rotulo;
}
