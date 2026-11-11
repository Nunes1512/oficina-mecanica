package com.oficina.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Visão financeira parcial: os gastos com estoque são entradas registradas,
 * não necessariamente pagamentos efetivados. Não representa lucro líquido.
 */
public record DashboardFinanceiroResponse(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal totalRecebido,
        BigDecimal custoEntradasEstoqueRegistrado,
        BigDecimal despesasBeneficiosRegistradas,
        BigDecimal outrasDespesasRegistradas,
        BigDecimal folhaSalarialMensalAtual,
        BigDecimal resultadoParcialAntesDaFolha) {
}
