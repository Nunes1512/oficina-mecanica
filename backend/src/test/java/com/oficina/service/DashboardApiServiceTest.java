package com.oficina.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.oficina.dto.DashboardResumoResponse;
import com.oficina.repository.DashboardIndicadoresRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class DashboardApiServiceTest {
    private DashboardIndicadoresRepository repository;
    private DashboardApiService service;

    @BeforeEach
    void preparar() {
        repository = mock(DashboardIndicadoresRepository.class);
        service = new DashboardApiService(repository);
    }

    @Test
    void semDatasUsaPrimeiroDiaDoMesAteHoje() {
        var periodo = service.validarPeriodo(null, null);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        assertEquals(hoje.withDayOfMonth(1), periodo.inicio());
        assertEquals(hoje, periodo.fim());
    }

    @Test
    void datasDevemSerEnviadasJuntas() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.resumo(LocalDate.now(), null)).getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void naoAceitaIntervaloInvertido() {
        var ex = assertThrows(ResponseStatusException.class,
                () -> service.resumo(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void naoAceitaConsultaMaiorQueDoisAnos() {
        assertThrows(ResponseStatusException.class,
                () -> service.resumo(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 11, 10)));
        verifyNoInteractions(repository);
    }

    @Test
    void agrupamentoDiarioBloqueiaIntervaloMuitoLongo() {
        assertThrows(ResponseStatusException.class,
                () -> service.evolucao(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 5, 1),
                        DashboardApiService.Agrupamento.DIA));
        verifyNoInteractions(repository);
    }

    @Test
    void evolucaoMensalChamaRepositorio() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 11, 10);
        when(repository.evolucao(inicio, fim, false)).thenReturn(List.of());
        assertTrue(service.evolucao(inicio, fim, DashboardApiService.Agrupamento.MES).isEmpty());
        verify(repository).evolucao(inicio, fim, false);
    }

    @Test
    void resumoRespeitaIntervaloInformado() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 10);
        DashboardResumoResponse esperado = new DashboardResumoResponse(
                inicio, fim, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, java.math.BigDecimal.ZERO);
        when(repository.resumo(inicio, fim)).thenReturn(esperado);
        assertSame(esperado, service.resumo(inicio, fim));
        verify(repository).resumo(inicio, fim);
    }
}
