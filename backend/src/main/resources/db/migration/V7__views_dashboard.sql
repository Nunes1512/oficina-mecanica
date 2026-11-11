-- Views auxiliares. A API moderna acessa DashboardIndicadoresRepository; V14 atualiza as views legadas.
CREATE OR REPLACE VIEW vw_os_atrasadas AS
SELECT os.id,os.numero,v.placa,v.previsao_entrega,os.status
  FROM ordem_servico os JOIN veiculo v ON v.id=os.veiculo_id
 WHERE os.status NOT IN ('CONCLUIDA','CANCELADA','ENCERRADA','VEICULO_RETIRADO')
   AND v.previsao_entrega < CURRENT_TIMESTAMP();

CREATE OR REPLACE VIEW vw_estoque_baixo AS
SELECT id,nome,fabricante,quantidade,estoque_minimo FROM peca
 WHERE quantidade <= estoque_minimo;

CREATE OR REPLACE VIEW vw_dashboard_resumo AS
SELECT
 (SELECT COALESCE(SUM(valor_total),0) FROM ordem_servico
   WHERE status IN ('CONCLUIDA','ENCERRADA') AND data_conclusao >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS faturamento,
 (SELECT COALESCE(SUM(valor_mao_obra),0) FROM ordem_servico
   WHERE status IN ('CONCLUIDA','ENCERRADA') AND data_conclusao >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS receita_servicos,
 (SELECT COALESCE(SUM(valor_pecas),0) FROM ordem_servico
   WHERE status IN ('CONCLUIDA','ENCERRADA') AND data_conclusao >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS receita_pecas,
 (SELECT COALESCE(SUM(quantidade * custo_unitario),0) FROM movimentacao_estoque
   WHERE tipo='ENTRADA' AND data >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS gastos_pecas,
 (SELECT COALESCE(SUM(salario),0) FROM funcionario WHERE status='ATIVO') AS gastos_salarios,
 (SELECT COALESCE(SUM(valor),0) FROM despesa
   WHERE tipo='BENEFICIO' AND data >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS gastos_beneficios,
 (SELECT COALESCE(SUM(valor),0) FROM despesa
   WHERE tipo='OUTRAS' AND data >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS outras_despesas,
 (SELECT COUNT(*) FROM veiculo
   WHERE status IN ('EM_MANUTENCAO','TESTES_FINAIS','AGUARDANDO_PECAS')) AS veiculos_em_manutencao,
 (SELECT COUNT(*) FROM ordem_servico WHERE status NOT IN ('CANCELADA','ENCERRADA','CONCLUIDA','VEICULO_RETIRADO')) AS os_abertas,
 (SELECT COUNT(*) FROM ordem_servico
   WHERE status IN ('CONCLUIDA','ENCERRADA') AND data_conclusao >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS os_concluidas;

CREATE OR REPLACE VIEW vw_dashboard_pizza AS
SELECT 'Receita de serviços' AS categoria,receita_servicos AS valor FROM vw_dashboard_resumo
UNION ALL SELECT 'Receita de peças',receita_pecas FROM vw_dashboard_resumo
UNION ALL SELECT 'Gastos com salários',gastos_salarios FROM vw_dashboard_resumo
UNION ALL SELECT 'Gastos com benefícios',gastos_beneficios FROM vw_dashboard_resumo
UNION ALL SELECT 'Gastos com peças',gastos_pecas FROM vw_dashboard_resumo
UNION ALL SELECT 'Outras despesas',outras_despesas FROM vw_dashboard_resumo;
