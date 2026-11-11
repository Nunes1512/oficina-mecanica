package com.oficina.controller;

import com.oficina.dto.DashboardEvolucaoResponse;
import com.oficina.dto.DashboardFinanceiroResponse;
import com.oficina.dto.DashboardResumoResponse;
import com.oficina.dto.DashboardStatusResponse;
import com.oficina.service.DashboardApiService;
import com.oficina.service.DashboardApiService.Agrupamento;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints JSON somente-leitura para o futuro front-end do dashboard. */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN_MASTER', 'GERENTE')")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardApiService dashboard;

    /** Indicadores operacionais atuais + totais do período; padrão: mês corrente. */
    @GetMapping("/resumo")
    public DashboardResumoResponse resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return dashboard.resumo(inicio, fim);
    }

    /** Valores recebidos e custos registrados; resultado parcial não é lucro líquido. */
    @GetMapping("/financeiro")
    public DashboardFinanceiroResponse financeiro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return dashboard.financeiro(inicio, fim);
    }

    /** Fotografia dos status das OS neste momento (sem filtro histórico). */
    @GetMapping("/status")
    public List<DashboardStatusResponse> status() {
        return dashboard.porStatus();
    }

    /** Evolução diária/mensal, incluindo datas sem movimento. */
    @GetMapping("/evolucao")
    public List<DashboardEvolucaoResponse> evolucao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "MES") Agrupamento agrupamento) {
        return dashboard.evolucao(inicio, fim, agrupamento);
    }
}
