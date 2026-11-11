-- Campo requerido por Cliente.dataCadastro (@Column criado_em).
ALTER TABLE cliente
 ADD COLUMN criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
CREATE INDEX idx_cliente_criado_em ON cliente(criado_em);
CREATE INDEX idx_veiculo_data_entrada ON veiculo(data_entrada);

-- Ajuste das views legadas para faturamento por pagamentos (receita efetivamente recebida).
CREATE OR REPLACE VIEW vw_dashboard_resumo AS
SELECT
 (SELECT COALESCE(SUM(p.valor),0) FROM pagamento_os p
  WHERE p.pago_em >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS faturamento,
 (SELECT COALESCE(SUM(o.valor_mao_obra),0) FROM ordem_servico o
   JOIN pagamento_os p ON p.os_id=o.id
   WHERE p.pago_em >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS receita_servicos,
 (SELECT COALESCE(SUM(o.valor_pecas),0) FROM ordem_servico o
   JOIN pagamento_os p ON p.os_id=o.id
   WHERE p.pago_em >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS receita_pecas,
 (SELECT COALESCE(SUM(quantidade * custo_unitario),0) FROM movimentacao_estoque
   WHERE tipo='ENTRADA' AND data >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS gastos_pecas,
 (SELECT COALESCE(SUM(salario),0) FROM funcionario WHERE status='ATIVO') AS gastos_salarios,
 (SELECT COALESCE(SUM(valor),0) FROM despesa
   WHERE tipo='BENEFICIO' AND data >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS gastos_beneficios,
 (SELECT COALESCE(SUM(valor),0) FROM despesa
   WHERE tipo='OUTRAS' AND data >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS outras_despesas,
 (SELECT COUNT(*) FROM veiculo
   WHERE status IN ('EM_MANUTENCAO','TESTES_FINAIS','AGUARDANDO_PECAS')) AS veiculos_em_manutencao,
 (SELECT COUNT(*) FROM ordem_servico
   WHERE status NOT IN ('CANCELADA','ENCERRADA','CONCLUIDA','VEICULO_RETIRADO')) AS os_abertas,
 (SELECT COUNT(*) FROM ordem_servico
   WHERE data_conclusao >= DATE_FORMAT(CURRENT_DATE(),'%Y-%m-01')) AS os_concluidas;

-- A view vw_dashboard_pizza de V7 referencia a view acima e já recebe os valores atualizados.
