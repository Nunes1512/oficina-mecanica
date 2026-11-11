package com.oficina.dto;

/** Fotografia dos status atuais das ordens de serviço. */
public record DashboardStatusResponse(String status, String descricao, long quantidade) {
}
