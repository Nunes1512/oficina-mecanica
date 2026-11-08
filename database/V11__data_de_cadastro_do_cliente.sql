-- Data em que o cliente foi cadastrado, para saber quantos clientes novos chegam por mês
ALTER TABLE cliente ADD COLUMN criado_em TIMESTAMP NOT NULL DEFAULT now();

-- Clientes antigos não têm essa data: usa a chegada do primeiro veículo como aproximação
UPDATE cliente c
   SET criado_em = v.primeira
  FROM (SELECT cliente_id, MIN(data_entrada) AS primeira FROM veiculo GROUP BY cliente_id) v
 WHERE v.cliente_id = c.id AND v.primeira < c.criado_em;

CREATE INDEX idx_cliente_criado_em ON cliente(criado_em);
