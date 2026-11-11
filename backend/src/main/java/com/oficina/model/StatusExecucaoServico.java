package com.oficina.model;

/** Andamento individual dos serviços aprovados na OS. */
public enum StatusExecucaoServico {
    PENDENTE("Pendente"), EM_ANDAMENTO("Em andamento"), CONCLUIDO("Concluído");

    private final String rotulo;
    StatusExecucaoServico(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
