package com.oficina.service;

import com.oficina.dto.DashboardEvolucaoResponse;
import com.oficina.dto.DashboardFinanceiroResponse;
import com.oficina.dto.DashboardResumoResponse;
import com.oficina.dto.DashboardStatusResponse;
import com.oficina.repository.DashboardIndicadoresRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardApiService {

    public enum Agrupamento { DIA, MES }

    private static final ZoneId FUSO_NEGOCIO = ZoneId.of("America/Sao_Paulo");
    private final DashboardIndicadoresRepository indicadores;

    public DashboardResumoResponse resumo(LocalDate inicio, LocalDate fim) {
        Periodo periodo = validarPeriodo(inicio, fim);
        return indicadores.resumo(periodo.inicio(), periodo.fim());
    }

    public DashboardFinanceiroResponse financeiro(LocalDate inicio, LocalDate fim) {
        Periodo periodo = validarPeriodo(inicio, fim);
        return indicadores.financeiro(periodo.inicio(), periodo.fim());
    }

    public List<DashboardStatusResponse> porStatus() {
        return indicadores.porStatus();
    }

    public List<DashboardEvolucaoResponse> evolucao(LocalDate inicio, LocalDate fim, Agrupamento agrupamento) {
        Periodo periodo = validarPeriodo(inicio, fim);
        if (agrupamento == null)
            throw erro("Agrupamento inválido: use DIA ou MES.");
        if (agrupamento == Agrupamento.DIA && ChronoUnit.DAYS.between(periodo.inicio(), periodo.fim()) > 365)
            throw erro("Para agrupar por DIA, utilize um intervalo de até 366 dias.");
        return indicadores.evolucao(periodo.inicio(), periodo.fim(), agrupamento == Agrupamento.DIA);
    }

    record Periodo(LocalDate inicio, LocalDate fim) { }

    Periodo validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null && fim == null) {
            LocalDate hoje = LocalDate.now(FUSO_NEGOCIO);
            return new Periodo(hoje.withDayOfMonth(1), hoje);
        }
        if (inicio == null || fim == null)
            throw erro("Informe inicio e fim juntos, no formato AAAA-MM-DD.");
        if (inicio.isAfter(fim))
            throw erro("A data de início não pode ser posterior à data de fim.");
        if (ChronoUnit.DAYS.between(inicio, fim) > 730)
            throw erro("Consulte um período de até 731 dias por requisição.");
        if (fim.equals(LocalDate.MAX))
            throw erro("A data final informada é inválida.");
        return new Periodo(inicio, fim);
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
