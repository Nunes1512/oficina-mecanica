package com.oficina.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Período = dia ou primeiro dia do mês, conforme o agrupamento pedido. */
public record DashboardEvolucaoResponse(
        LocalDate periodo,
        long osAbertas,
        long osConcluidasTecnicamente,
        long veiculosRetirados,
        long pagamentos,
        BigDecimal totalRecebido) {
}
