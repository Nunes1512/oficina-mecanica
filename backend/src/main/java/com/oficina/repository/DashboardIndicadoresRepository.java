package com.oficina.repository;

import com.oficina.dto.DashboardEvolucaoResponse;
import com.oficina.dto.DashboardFinanceiroResponse;
import com.oficina.dto.DashboardResumoResponse;
import com.oficina.dto.DashboardStatusResponse;
import com.oficina.model.StatusOs;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas do Dashboard compatíveis com MariaDB 10.11 (XAMPP). */
@Repository
@RequiredArgsConstructor
public class DashboardIndicadoresRepository {
    private final NamedParameterJdbcTemplate jdbc;

    private static final String SQL_RESUMO = """
        SELECT
          (SELECT COUNT(*) FROM ordem_servico o
             WHERE o.data_abertura >= :inicio AND o.data_abertura < :fimExclusivo) AS abertas_periodo,
          (SELECT COUNT(*) FROM ordem_servico o
             WHERE o.data_conclusao >= :inicio AND o.data_conclusao < :fimExclusivo) AS concluidas_periodo,
          (SELECT COUNT(*) FROM ordem_servico o
             WHERE o.status NOT IN ('ENCERRADA','CANCELADA')) AS em_aberto_total,
          (SELECT COUNT(*) FROM ordem_servico o WHERE o.status = 'ENCERRADA') AS encerradas_total,
          (SELECT COUNT(*) FROM ordem_servico o
             WHERE o.status IN ('AGUARDANDO_EXECUCAO','EM_ANDAMENTO','AGUARDANDO_PECAS')) AS aguardando_ou_executando,
          (SELECT COUNT(*) FROM ordem_servico o WHERE o.status = 'AGUARDANDO_PAGAMENTO') AS aguardando_pagamento,
          (SELECT COUNT(*) FROM ordem_servico o WHERE o.status = 'AGUARDANDO_RETIRADA') AS aguardando_retirada,
          (SELECT COUNT(*) FROM peca p WHERE p.quantidade <= p.estoque_minimo) AS pecas_baixas,
          (SELECT COUNT(*) FROM pagamento_os p
             WHERE p.pago_em >= :inicio AND p.pago_em < :fimExclusivo) AS pagamentos_periodo,
          (SELECT COUNT(*) FROM retirada_os r
             WHERE r.retirado_em >= :inicio AND r.retirado_em < :fimExclusivo) AS retiradas_periodo,
          (SELECT COALESCE(SUM(p.valor),0) FROM pagamento_os p
             WHERE p.pago_em >= :inicio AND p.pago_em < :fimExclusivo) AS recebido_periodo
        """;

    public DashboardResumoResponse resumo(LocalDate inicio, LocalDate fim) {
        var parametros = parametros(inicio, fim);
        return jdbc.queryForObject(SQL_RESUMO, parametros, (rs, rowNum) -> new DashboardResumoResponse(
                inicio, fim,
                rs.getLong("abertas_periodo"), rs.getLong("concluidas_periodo"),
                rs.getLong("em_aberto_total"), rs.getLong("encerradas_total"),
                rs.getLong("aguardando_ou_executando"), rs.getLong("aguardando_pagamento"),
                rs.getLong("aguardando_retirada"), rs.getLong("pecas_baixas"),
                rs.getLong("pagamentos_periodo"), rs.getLong("retiradas_periodo"),
                valor(rs.getBigDecimal("recebido_periodo"))));
    }

    public DashboardFinanceiroResponse financeiro(LocalDate inicio, LocalDate fim) {
        String sql = """
            SELECT
              (SELECT COALESCE(SUM(p.valor),0) FROM pagamento_os p
                 WHERE p.pago_em >= :inicio AND p.pago_em < :fimExclusivo) AS recebido,
              (SELECT COALESCE(SUM(m.quantidade * COALESCE(m.custo_unitario, 0)),0)
                 FROM movimentacao_estoque m
                 WHERE m.tipo = 'ENTRADA' AND m.data >= :inicio AND m.data < :fimExclusivo) AS entradas_estoque,
              (SELECT COALESCE(SUM(d.valor),0) FROM despesa d
                 WHERE d.tipo = 'BENEFICIO'
                   AND d.data >= CAST(:inicio AS date)
                   AND d.data <= CAST(:fimInclusivo AS date)) AS beneficios,
              (SELECT COALESCE(SUM(d.valor),0) FROM despesa d
                 WHERE d.tipo = 'OUTRAS'
                   AND d.data >= CAST(:inicio AS date)
                   AND d.data <= CAST(:fimInclusivo AS date)) AS outras,
              (SELECT COALESCE(SUM(f.salario),0) FROM funcionario f
                 WHERE f.status = 'ATIVO') AS folha_atual
            """;
        return jdbc.queryForObject(sql, parametros(inicio, fim), (rs, rowNum) -> {
            BigDecimal recebido = valor(rs.getBigDecimal("recebido"));
            BigDecimal estoque = valor(rs.getBigDecimal("entradas_estoque"));
            BigDecimal beneficios = valor(rs.getBigDecimal("beneficios"));
            BigDecimal outras = valor(rs.getBigDecimal("outras"));
            BigDecimal folha = valor(rs.getBigDecimal("folha_atual"));
            return new DashboardFinanceiroResponse(inicio, fim, recebido, estoque, beneficios,
                    outras, folha, recebido.subtract(estoque).subtract(beneficios).subtract(outras));
        });
    }

    public List<DashboardStatusResponse> porStatus() {
        String sql = "SELECT status, COUNT(*) AS quantidade FROM ordem_servico GROUP BY status ORDER BY status";
        return jdbc.query(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            String status = rs.getString("status");
            return new DashboardStatusResponse(status, StatusOs.valueOf(status).getRotulo(),
                    rs.getLong("quantidade"));
        });
    }

    /** CTE recursiva MariaDB. No máximo 366 linhas por DIA, ou 25 por MÊS. */
    public List<DashboardEvolucaoResponse> evolucao(LocalDate inicio, LocalDate fim, boolean porDia) {
        String unidade = porDia ? "DAY" : "MONTH";
        LocalDate primeiroPeriodo = porDia ? inicio : inicio.withDayOfMonth(1);
        LocalDate ultimoPeriodo = porDia ? fim : fim.withDayOfMonth(1);
        String sql = """
            WITH RECURSIVE periodos AS (
                SELECT CAST(:primeiroPeriodo AS DATE) AS periodo
                UNION ALL
                SELECT DATE_ADD(periodo, INTERVAL 1 %s)
                  FROM periodos
                 WHERE periodo < CAST(:ultimoPeriodo AS DATE)
            )
            SELECT f.periodo,
                (SELECT COUNT(*) FROM ordem_servico o
                   WHERE o.data_abertura >= GREATEST(f.periodo,:inicio)
                     AND o.data_abertura < LEAST(DATE_ADD(f.periodo, INTERVAL 1 %s),:fimExclusivo)) AS abertas,
                (SELECT COUNT(*) FROM ordem_servico o
                   WHERE o.data_conclusao >= GREATEST(f.periodo,:inicio)
                     AND o.data_conclusao < LEAST(DATE_ADD(f.periodo, INTERVAL 1 %s),:fimExclusivo)) AS concluidas,
                (SELECT COUNT(*) FROM retirada_os r
                   WHERE r.retirado_em >= GREATEST(f.periodo,:inicio)
                     AND r.retirado_em < LEAST(DATE_ADD(f.periodo, INTERVAL 1 %s),:fimExclusivo)) AS retiradas,
                (SELECT COUNT(*) FROM pagamento_os p
                   WHERE p.pago_em >= GREATEST(f.periodo,:inicio)
                     AND p.pago_em < LEAST(DATE_ADD(f.periodo, INTERVAL 1 %s),:fimExclusivo)) AS pagamentos,
                (SELECT COALESCE(SUM(p.valor),0) FROM pagamento_os p
                   WHERE p.pago_em >= GREATEST(f.periodo,:inicio)
                     AND p.pago_em < LEAST(DATE_ADD(f.periodo, INTERVAL 1 %s),:fimExclusivo)) AS recebido
            FROM periodos f ORDER BY f.periodo
            """.formatted(unidade, unidade, unidade, unidade, unidade, unidade);
        MapSqlParameterSource parametros = parametros(inicio, fim)
                .addValue("primeiroPeriodo", Date.valueOf(primeiroPeriodo))
                .addValue("ultimoPeriodo", Date.valueOf(ultimoPeriodo));
        return jdbc.query(sql, parametros, (rs, rowNum) -> new DashboardEvolucaoResponse(
                rs.getDate("periodo").toLocalDate(),
                rs.getLong("abertas"), rs.getLong("concluidas"),
                rs.getLong("retiradas"), rs.getLong("pagamentos"),
                valor(rs.getBigDecimal("recebido"))));
    }

    private MapSqlParameterSource parametros(LocalDate inicio, LocalDate fim) {
        return new MapSqlParameterSource()
                .addValue("inicio", Timestamp.valueOf(inicio.atStartOfDay()))
                .addValue("fimInclusivo", Timestamp.valueOf(fim.atStartOfDay()))
                .addValue("fimExclusivo", Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
