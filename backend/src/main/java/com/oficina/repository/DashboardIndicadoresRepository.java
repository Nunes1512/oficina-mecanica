package com.oficina.repository;

import com.oficina.dto.DashboardEvolucaoResponse;
import com.oficina.dto.DashboardFinanceiroResponse;
import com.oficina.dto.DashboardResumoResponse;
import com.oficina.dto.DashboardStatusResponse;
import com.oficina.model.StatusOs;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas agregadas: utiliza o esquema vigente (V10 a V12), sem fazer fetch de
 * entidades JPA ou carregar comprovantes (BYTEA) em memória.
 *
 * Faturamento recebido vem de pagamento_os; contar OS CONCLUIDA como receita
 * seria incorreto após a introdução das etapas de pagamento e encerramento.
 */
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

    /**
     * A receita vem dos pagamentos efetivos; demais lançamentos são apenas
     * custos/despesas registrados. Folha salarial é a fotografia mensal ATUAL.
     */
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

    /** Mostra apenas status com pelo menos uma OS; o gráfico pode omitir zeros. */
    public List<DashboardStatusResponse> porStatus() {
        String sql = "SELECT status, COUNT(*) AS quantidade FROM ordem_servico GROUP BY status ORDER BY status";
        return jdbc.query(sql, new MapSqlParameterSource(), (rs, rowNum) -> {
            String status = rs.getString("status");
            return new DashboardStatusResponse(status, StatusOs.valueOf(status).getRotulo(),
                    rs.getLong("quantidade"));
        });
    }

    /** Períodos vazios aparecem como zero usando generate_series do PostgreSQL. */
    public List<DashboardEvolucaoResponse> evolucao(LocalDate inicio, LocalDate fim, boolean porDia) {
        // Ambos os valores são literais fixos no código, nunca texto fornecido pelo usuário.
        String unidade = porDia ? "day" : "month";
        String sql = """
            WITH faixas AS (
                SELECT CAST(g.periodo AS DATE) AS periodo,
                       GREATEST(g.periodo, CAST(:inicio AS timestamp)) AS inicio_faixa,
                       LEAST(g.periodo + INTERVAL '1 %s', CAST(:fimExclusivo AS timestamp)) AS fim_faixa
                  FROM generate_series(
                       date_trunc('%s', CAST(:inicio AS timestamp)),
                       date_trunc('%s', CAST(:fimInclusivo AS timestamp)),
                       INTERVAL '1 %s') AS g(periodo)
            )
            SELECT f.periodo,
                (SELECT COUNT(*) FROM ordem_servico o
                   WHERE o.data_abertura >= f.inicio_faixa AND o.data_abertura < f.fim_faixa) AS abertas,
                (SELECT COUNT(*) FROM ordem_servico o
                   WHERE o.data_conclusao >= f.inicio_faixa AND o.data_conclusao < f.fim_faixa) AS concluidas,
                (SELECT COUNT(*) FROM retirada_os r
                   WHERE r.retirado_em >= f.inicio_faixa AND r.retirado_em < f.fim_faixa) AS retiradas,
                (SELECT COUNT(*) FROM pagamento_os p
                   WHERE p.pago_em >= f.inicio_faixa AND p.pago_em < f.fim_faixa) AS pagamentos,
                (SELECT COALESCE(SUM(p.valor),0) FROM pagamento_os p
                   WHERE p.pago_em >= f.inicio_faixa AND p.pago_em < f.fim_faixa) AS recebido
            FROM faixas f ORDER BY f.periodo
            """.formatted(unidade, unidade, unidade, unidade);
        return jdbc.query(sql, parametros(inicio, fim), (rs, rowNum) -> new DashboardEvolucaoResponse(
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
