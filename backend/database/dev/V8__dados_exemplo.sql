CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE FUNCTION pg_temp.mes_frac(f NUMERIC) RETURNS TIMESTAMPTZ AS $$
    SELECT date_trunc('month', now()) + (now() - date_trunc('month', now())) * f::double precision;
$$ LANGUAGE sql;

INSERT INTO usuario (email, senha, funcao_id)
SELECT u.email, crypt('Senha@123', gen_salt('bf', 10)), f.id
  FROM (VALUES
        ('admin@oficina.com',   'ROLE_ADMIN_MASTER'),
        ('gerente@oficina.com', 'ROLE_GERENTE'),
        ('marcos@oficina.com',  'ROLE_FUNCIONARIO'),
        ('rafael@oficina.com',  'ROLE_FUNCIONARIO'),
        ('juliana@oficina.com', 'ROLE_FUNCIONARIO')
       ) AS u(email, perfil)
  JOIN funcao f ON f.nome = u.perfil;

INSERT INTO funcionario (nome, cpf, telefone, email, cargo, salario, status, usuario_id)
SELECT v.nome, v.cpf, v.telefone, v.email, v.cargo, v.salario, 'ATIVO', u.id
  FROM (VALUES
        ('Fernanda Alves',  '900.000.000-01', '(11) 90000-0001', 'gerente@oficina.com', 'Gerente',                5200.00),
        ('Marcos Oliveira', '900.000.000-02', '(11) 90000-0002', 'marcos@oficina.com',  'Mecânico Chefe',         3800.00),
        ('Rafael Souza',    '900.000.000-03', '(11) 90000-0003', 'rafael@oficina.com',  'Mecânico',               2900.00),
        ('Juliana Costa',   '900.000.000-04', '(11) 90000-0004', 'juliana@oficina.com', 'Eletricista Automotiva', 3100.00),
        ('Pedro Lima',      '900.000.000-05', '(11) 90000-0005', NULL,                  'Auxiliar de Mecânica',   1900.00)
       ) AS v(nome, cpf, telefone, email, cargo, salario)
  LEFT JOIN usuario u ON u.email = v.email;

INSERT INTO cliente (nome, cpf, telefone, email, endereco) VALUES
('Roberto Almeida',   '800.000.000-01', '(11) 98000-0001', 'roberto.almeida@email.com',   'Rua das Acácias, 120'),
('Camila Ferreira',   '800.000.000-02', '(11) 98000-0002', 'camila.ferreira@email.com',   'Av. Brasil, 845'),
('Lucas Martins',     '800.000.000-03', '(11) 98000-0003', 'lucas.martins@email.com',     'Rua Sete de Setembro, 56'),
('Patrícia Gomes',    '800.000.000-04', '(11) 98000-0004', 'patricia.gomes@email.com',    'Rua das Palmeiras, 310'),
('Anderson Ribeiro',  '800.000.000-05', '(11) 98000-0005', 'anderson.ribeiro@email.com',  'Av. Paulista, 1500'),
('Beatriz Carvalho',  '800.000.000-06', '(11) 98000-0006', 'beatriz.carvalho@email.com',  'Rua do Comércio, 77'),
('Eduardo Teixeira',  '800.000.000-07', '(11) 98000-0007', 'eduardo.teixeira@email.com',  'Rua XV de Novembro, 402'),
('Larissa Mendes',    '800.000.000-08', '(11) 98000-0008', 'larissa.mendes@email.com',    'Rua das Flores, 29');

INSERT INTO peca (nome, fabricante, quantidade, valor_unitario, estoque_minimo) VALUES
('Filtro de óleo',              'Mann',    40,  35.00, 10),
('Filtro de ar',                'Fram',    25,  48.00,  8),
('Pastilha de freio dianteira', 'Cobreq',   8, 165.00,  6),
('Disco de freio',              'Fremax',   5, 289.90,  4),
('Óleo 5W30 sintético 1L',      'Mobil',   60,  54.90, 20),
('Vela de ignição',             'NGK',     32,  29.90, 12),
('Correia dentada',             'Gates',    7, 215.00,  3),
('Bateria 60Ah',                'Moura',    5, 489.00,  3),
('Amortecedor dianteiro',       'Monroe',   9, 342.00,  4),
('Lâmpada de farol H4',         'Philips', 18,  39.90,  6);

INSERT INTO movimentacao_estoque (peca_id, tipo, quantidade, custo_unitario, observacao, data)
SELECT id, 'ENTRADA', quantidade, ROUND(valor_unitario * 0.6, 2), 'Estoque inicial', now() - interval '90 days'
  FROM peca;

INSERT INTO veiculo (marca, modelo, ano, cor, placa, quilometragem, data_entrada, previsao_entrega, data_entrega, status, cliente_id)
SELECT v.marca, v.modelo, v.ano, v.cor, v.placa, v.km, v.entrada, v.previsao, v.entrega, v.status, c.id
  FROM (VALUES
        ('Fiat',       'Uno',     2014, 'Prata',    'ABC1A11',  98000, now() - interval '2 days', now() + interval '3 days', NULL::timestamptz, 'AGUARDANDO_ANALISE', '800.000.000-01'),
        ('Volkswagen', 'Gol',     2017, 'Branco',   'BRA2B22',  76500, now() - interval '5 days', now() + interval '2 days', NULL, 'AGUARDANDO_PECAS',  '800.000.000-02'),
        ('Chevrolet',  'Onix',    2019, 'Preto',    'CAR3C33',  54200, now() - interval '6 days', now() + interval '1 day',  NULL, 'EM_MANUTENCAO',     '800.000.000-03'),
        ('Honda',      'Civic',   2016, 'Cinza',    'DEF4D44', 112300, now() - interval '9 days', now() - interval '2 days', NULL, 'EM_MANUTENCAO',     '800.000.000-04'),
        ('Toyota',     'Corolla', 2018, 'Prata',    'GHI5E55',  68900, now() - interval '4 days', now() + interval '1 day',  NULL, 'TESTES_FINAIS',     '800.000.000-05'),
        ('Hyundai',    'HB20',    2020, 'Vermelho', 'JKL6F66',  41000, now() - interval '7 days', now(),                     NULL, 'CONCLUIDO',         '800.000.000-06'),
        ('Ford',       'Ka',      2015, 'Azul',     'MNO7G77',  88000, pg_temp.mes_frac(0.30), pg_temp.mes_frac(0.58), pg_temp.mes_frac(0.60), 'ENTREGUE', '800.000.000-07'),
        ('Renault',    'Sandero', 2016, 'Branco',   'PQR8H88',  93500, pg_temp.mes_frac(0.32), pg_temp.mes_frac(0.59), pg_temp.mes_frac(0.61), 'ENTREGUE', '800.000.000-08'),
        ('Fiat',       'Argo',    2021, 'Cinza',    'STU9I99',  32000, pg_temp.mes_frac(0.33), pg_temp.mes_frac(0.60), pg_temp.mes_frac(0.62), 'ENTREGUE', '800.000.000-01')
       ) AS v(marca, modelo, ano, cor, placa, km, entrada, previsao, entrega, status, cpf)
  JOIN cliente c ON c.cpf = v.cpf;

INSERT INTO ordem_servico (numero, veiculo_id, funcionario_id, problema, diagnostico, valor_mao_obra, status, data_abertura, data_conclusao)
SELECT o.numero, v.id, f.id, o.problema, o.diagnostico, o.mao_obra, o.status, o.abertura, o.conclusao
  FROM (VALUES
        ('OS-0001', 'ABC1A11', '900.000.000-02', 'Barulho na suspensão ao passar em buracos', NULL, 0, 'ABERTA',           now() - interval '2 days', NULL::timestamptz),
        ('OS-0002', 'BRA2B22', '900.000.000-03', 'Freios com ruído e pedal baixo', 'Pastilhas e discos dianteiros desgastados', 280, 'AGUARDANDO_PECAS', now() - interval '5 days', NULL),
        ('OS-0003', 'CAR3C33', '900.000.000-04', 'Luz da injeção acesa', 'Velas de ignição com desgaste', 350, 'EM_ANDAMENTO', now() - interval '6 days', NULL),
        ('OS-0004', 'DEF4D44', '900.000.000-02', 'Revisão e troca da correia dentada', 'Correia com folga excessiva', 650, 'EM_ANDAMENTO', now() - interval '9 days', NULL),
        ('OS-0005', 'GHI5E55', '900.000.000-02', 'Bateria descarregando', 'Bateria com vida útil esgotada', 120, 'EM_ANDAMENTO', now() - interval '4 days', NULL),
        ('OS-0006', 'JKL6F66', '900.000.000-03', 'Veículo pulando em curvas', 'Amortecedores dianteiros vencidos', 480, 'CONCLUIDA', now() - interval '7 days', now() - interval '1 day'),
        ('OS-0007', 'MNO7G77', '900.000.000-02', 'Freios rangendo', 'Pastilhas e discos com desgaste acima do limite', 380, 'CONCLUIDA', pg_temp.mes_frac(0.30), pg_temp.mes_frac(0.55)),
        ('OS-0008', 'PQR8H88', '900.000.000-03', 'Dificuldade para ligar e farol fraco', 'Bateria fraca e lâmpadas queimadas', 150, 'CONCLUIDA', pg_temp.mes_frac(0.32), pg_temp.mes_frac(0.56)),
        ('OS-0009', 'STU9I99', '900.000.000-02', 'Revisão dos 40 mil km', 'Revisão completa executada', 420, 'CONCLUIDA', pg_temp.mes_frac(0.33), pg_temp.mes_frac(0.58))
       ) AS o(numero, placa, cpf_func, problema, diagnostico, mao_obra, status, abertura, conclusao)
  JOIN veiculo v     ON v.placa = o.placa
  JOIN funcionario f ON f.cpf   = o.cpf_func;

INSERT INTO item_os (os_id, peca_id, quantidade, valor_unitario)
SELECT o.id, p.id, i.qtd, p.valor_unitario
  FROM (VALUES
        ('OS-0003', 'Vela de ignição',             4),
        ('OS-0004', 'Correia dentada',             1),
        ('OS-0004', 'Filtro de óleo',              1),
        ('OS-0004', 'Óleo 5W30 sintético 1L',      4),
        ('OS-0005', 'Bateria 60Ah',                1),
        ('OS-0006', 'Amortecedor dianteiro',       2),
        ('OS-0007', 'Pastilha de freio dianteira', 2),
        ('OS-0007', 'Disco de freio',              2),
        ('OS-0008', 'Bateria 60Ah',                2),
        ('OS-0008', 'Lâmpada de farol H4',         2),
        ('OS-0009', 'Filtro de óleo',              1),
        ('OS-0009', 'Filtro de ar',                1),
        ('OS-0009', 'Óleo 5W30 sintético 1L',      4),
        ('OS-0009', 'Vela de ignição',             4)
       ) AS i(numero, peca, qtd)
  JOIN ordem_servico o ON o.numero = i.numero
  JOIN peca p          ON p.nome   = i.peca;

WITH compras(nome, qtd, custo) AS (
    VALUES ('Óleo 5W30 sintético 1L', 24,  32.00),
           ('Filtro de óleo',         20,  21.00),
           ('Vela de ignição',        16,  17.50),
           ('Correia dentada',         4, 130.00),
           ('Amortecedor dianteiro',   6, 205.00)
),
entrada AS (
    INSERT INTO movimentacao_estoque (peca_id, tipo, quantidade, custo_unitario, observacao, data)
    SELECT p.id, 'ENTRADA', c.qtd, c.custo, 'Compra de reposição', pg_temp.mes_frac(0.30)
      FROM compras c
      JOIN peca p ON p.nome = c.nome
    RETURNING peca_id, quantidade
)
UPDATE peca p
   SET quantidade = p.quantidade + e.quantidade
  FROM entrada e
 WHERE p.id = e.peca_id;

DO $$
DECLARE
    i           INT;
    v_veiculo   BIGINT;
    v_concl     TIMESTAMP;
    v_abert     TIMESTAMP;
    v_entrega   TIMESTAMP;
    v_marcas    TEXT[] := ARRAY['Fiat','Volkswagen','Chevrolet','Ford','Honda','Toyota'];
    v_modelos   TEXT[] := ARRAY['Palio','Polo','Prisma','Fiesta','Fit','Etios'];
    v_cores     TEXT[] := ARRAY['Branco','Prata','Preto','Vermelho','Azul','Cinza'];
    v_problemas TEXT[] := ARRAY['Revisão periódica','Troca de óleo e filtros','Barulho no motor',
                                'Problema na suspensão','Freios com ruído','Ar-condicionado sem gelar'];
    v_clientes  BIGINT[];
    v_mecanicos BIGINT[];
BEGIN
    SELECT array_agg(id ORDER BY id) INTO v_clientes FROM cliente;
    SELECT array_agg(id ORDER BY cpf) INTO v_mecanicos
      FROM funcionario WHERE cpf IN ('900.000.000-02', '900.000.000-03', '900.000.000-04');

    FOR i IN 1..38 LOOP
        IF i <= 14 THEN
            v_concl := date_trunc('month', now()) + (now() - date_trunc('month', now())) * (i / 15.0)::double precision;
        ELSE
            v_concl := now() - make_interval(days => 20 + (i - 14) * 14);
        END IF;
        v_abert   := v_concl - interval '3 days';
        v_entrega := LEAST(v_concl + interval '4 hours', now()::timestamp);

        INSERT INTO veiculo (marca, modelo, ano, cor, placa, quilometragem,
                             data_entrada, previsao_entrega, data_entrega, status, cliente_id)
        VALUES (v_marcas[1 + i % 6], v_modelos[1 + i % 6], 2012 + i % 12, v_cores[1 + (i + 2) % 6],
                'HST' || lpad(i::text, 4, '0'), 20000 + i * 3100,
                v_abert, v_concl, v_entrega, 'ENTREGUE',
                v_clientes[1 + i % array_length(v_clientes, 1)])
        RETURNING id INTO v_veiculo;

        INSERT INTO ordem_servico (numero, veiculo_id, funcionario_id, problema, diagnostico,
                                   valor_mao_obra, status, data_abertura, data_conclusao)
        VALUES ('OS-H' || lpad(i::text, 4, '0'), v_veiculo, v_mecanicos[1 + i % 3],
                v_problemas[1 + i % 6], 'Serviço executado e testado',
                900 + ((i * 263) % 1900), 'CONCLUIDA', v_abert, v_concl);
    END LOOP;
END $$;

INSERT INTO solicitacao_peca (funcionario_id, peca_id, quantidade, justificativa, status, data)
SELECT f.id, p.id, s.qtd, s.justificativa, s.status, pg_temp.mes_frac(s.frac)
  FROM (VALUES
        ('900.000.000-03', 'Pastilha de freio dianteira', 6,  'Estoque no mínimo e OS-0002 aguardando peças', 'PENDENTE', 0.97),
        ('900.000.000-03', 'Disco de freio',              4,  'OS-0002 aguardando os discos dianteiros',     'PENDENTE', 0.97),
        ('900.000.000-02', 'Correia dentada',             2,  'Reposição após a OS-0004',                    'APROVADA', 0.60),
        ('900.000.000-04', 'Vela de ignição',             12, 'Reposição para as próximas revisões',         'COMPRADA', 0.40),
        ('900.000.000-02', 'Amortecedor dianteiro',       4,  'Pedido sem OS vinculada',                     'RECUSADA', 0.20)
       ) AS s(cpf, peca, qtd, justificativa, status, frac)
  JOIN funcionario f ON f.cpf  = s.cpf
  JOIN peca p        ON p.nome = s.peca;

INSERT INTO despesa (tipo, descricao, valor, data)
SELECT d.tipo, d.descricao, d.valor, (date_trunc('month', now()) - make_interval(months => m))::date
  FROM generate_series(0, 2) AS m
 CROSS JOIN (VALUES
        ('BENEFICIO', 'Vale-refeição',            1800.00),
        ('BENEFICIO', 'Plano de saúde',           2100.00),
        ('OUTRAS',    'Aluguel do galpão',        2500.00),
        ('OUTRAS',    'Energia elétrica',          680.00),
        ('OUTRAS',    'Internet e telefone',       220.00),
        ('OUTRAS',    'Ferramentas e consumíveis', 540.00)
       ) AS d(tipo, descricao, valor);

INSERT INTO timeline_veiculo (veiculo_id, evento, descricao, data)
SELECT v.id, t.evento, t.descricao, v.data_entrada + t.apos
  FROM (VALUES
        ('BRA2B22', 'DIAGNOSTICO',      'Pastilhas e discos dianteiros desgastados',   interval '1 day'),
        ('BRA2B22', 'SOLICITACAO_PECA', 'Solicitadas pastilhas e discos de freio',     interval '1 day 2 hours'),
        ('BRA2B22', 'MUDANCA_STATUS',   'Veículo aguardando a chegada das peças',      interval '1 day 3 hours'),
        ('CAR3C33', 'DIAGNOSTICO',      'Velas de ignição com desgaste',               interval '1 day'),
        ('CAR3C33', 'INICIO_CONSERTO',  'Início da troca das velas',                   interval '2 days'),
        ('DEF4D44', 'DIAGNOSTICO',      'Correia dentada com folga excessiva',         interval '1 day'),
        ('DEF4D44', 'SOLICITACAO_PECA', 'Solicitada correia dentada',                  interval '1 day 1 hour'),
        ('DEF4D44', 'APROVACAO_PECA',   'Correia dentada aprovada pelo gerente',       interval '2 days'),
        ('DEF4D44', 'INICIO_CONSERTO',  'Início da troca da correia',                  interval '3 days'),
        ('GHI5E55', 'DIAGNOSTICO',      'Bateria com vida útil esgotada',              interval '1 day'),
        ('GHI5E55', 'INICIO_CONSERTO',  'Início da troca da bateria',                  interval '1 day 4 hours'),
        ('GHI5E55', 'MUDANCA_STATUS',   'Veículo em testes finais',                    interval '3 days'),
        ('JKL6F66', 'DIAGNOSTICO',      'Amortecedores dianteiros vencidos',           interval '1 day'),
        ('JKL6F66', 'INICIO_CONSERTO',  'Início da troca dos amortecedores',           interval '2 days'),
        ('JKL6F66', 'CONCLUSAO',        'Serviço concluído, aguardando retirada',      interval '5 days')
       ) AS t(placa, evento, descricao, apos)
  JOIN veiculo v ON v.placa = t.placa;

UPDATE timeline_veiculo t
   SET data = v.data_entrada
  FROM veiculo v
 WHERE t.veiculo_id = v.id AND t.evento = 'ENTRADA';

UPDATE movimentacao_estoque m
   SET data = o.data_abertura
  FROM ordem_servico o
 WHERE m.os_id = o.id AND m.tipo = 'SAIDA';

DELETE FROM notificacao;

SELECT fn_notificar_gestao('ESTOQUE_BAIXO',
       'Estoque baixo: ' || nome || ' (' || quantidade || ' un., mínimo ' || estoque_minimo || ')')
  FROM vw_estoque_baixo;

SELECT fn_notificar_gestao('OS_ATRASADA', 'OS ' || numero || ' atrasada (veículo ' || placa || ')')
  FROM vw_os_atrasadas;

SELECT fn_notificar_gestao('SOLICITACAO_PENDENTE', 'Solicitação de peça pendente (#' || id || ')')
  FROM solicitacao_peca WHERE status = 'PENDENTE';

SELECT fn_notificar_gestao('VEICULO_AGUARDANDO_PECA', 'Veículo ' || placa || ' aguardando peças')
  FROM veiculo WHERE status = 'AGUARDANDO_PECAS';

SELECT fn_notificar_gestao('SERVICO_CONCLUIDO', 'OS ' || numero || ' concluída')
  FROM ordem_servico WHERE numero = 'OS-0006';

UPDATE notificacao SET data = now() - (id % 6)::int * interval '2 hours';
UPDATE notificacao SET lida = TRUE WHERE tipo = 'SERVICO_CONCLUIDO';

DROP FUNCTION pg_temp.mes_frac(NUMERIC);
