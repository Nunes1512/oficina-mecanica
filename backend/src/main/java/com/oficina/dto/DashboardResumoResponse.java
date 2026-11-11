package com.oficina.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Indicadores operacionais e recebimentos reais, sem expor entidades JPA. */
public record DashboardResumoResponse(
        LocalDate inicio,
        LocalDate fim,
        long osAbertasNoPeriodo,
        long osConcluidasTecnicamenteNoPeriodo,
        long osEmAbertoAtualmente,
        long osEncerradasAtualmente,
        long osAguardandoExecucao,
        long osAguardandoPagamento,
        long osAguardandoRetirada,
        long pecasEmEstoqueBaixo,
        long pagamentosNoPeriodo,
        long veiculosRetiradosNoPeriodo,
        BigDecimal totalRecebidoNoPeriodo) {
}
