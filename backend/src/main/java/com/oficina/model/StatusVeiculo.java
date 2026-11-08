package com.oficina.model;

/**
 * Etapas do reparo, na ordem em que acontecem. A ordem de declaração É a ordem do fluxo:
 * o sistema só deixa o status andar para a etapa vizinha (ver FluxoStatusVeiculo).
 */
public enum StatusVeiculo {
    AGUARDANDO_ANALISE("Aguardando análise"),
    AGUARDANDO_PECAS("Aguardando peças"),
    EM_MANUTENCAO("Em manutenção"),
    TESTES_FINAIS("Em teste"),
    CONCLUIDO("Concluído"),
    ENTREGUE("Entregue");

    private final String rotulo;

    StatusVeiculo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Próxima etapa do fluxo, ou null se esta já é a última. */
    public StatusVeiculo proximo() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    /** Etapa anterior do fluxo, ou null se esta é a primeira. */
    public StatusVeiculo anterior() {
        return ordinal() > 0 ? values()[ordinal() - 1] : null;
    }
}
