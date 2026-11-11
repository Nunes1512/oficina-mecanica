-- OFICINA MECANICA - POVOAMENTO DE DEMONSTRACAO (MariaDB 10.11 / XAMPP)
-- Somente para ambiente de TESTES, nunca usar contas ficticias em producao.
-- PRE-REQUISITOS: Aplicacao ja iniciou; Flyway V1..V14 aplicadas; primeiro Admin criado.
-- O script NAO altera o admin e NAO cria OS: voce podera testar o fluxo desde a abertura.
-- Pode executar novamente: os INSERTs verificam registros existentes para evitar duplicacao.
-- CPF e placas sao sinteticos para teste; e-mails .test nao recebem mensagens.
-- Senhas: somente hashes BCrypt (custo 12). Credenciais temporarias ficam em arquivo separado.
USE oficina_mecanica;
SET NAMES utf8mb4;
START TRANSACTION;

-- 1. Usuarios com os perfis exigidos pelo controle de acesso
-- Marina Farias / ROLE_GERENTE
INSERT INTO usuario (email,senha,ativo,bloqueado,funcao_id)
SELECT 'gerente.demo@oficina.test', '$2y$12$x5udSimR1wTA57ycLwvVDeDW5ATQtYH2m2hgmiFuyVyA7kIkDq5Hi', 1, 0, f.id
  FROM funcao f WHERE f.nome='ROLE_GERENTE'
    AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email='gerente.demo@oficina.test');

-- Bruno Nogueira / ROLE_MECANICO
INSERT INTO usuario (email,senha,ativo,bloqueado,funcao_id)
SELECT 'mecanico1.demo@oficina.test', '$2y$12$y.XqoX6ahqYt2yekBDbxyOHiVtqoBKVmAingOBbESZG8rS.VUKK6W', 1, 0, f.id
  FROM funcao f WHERE f.nome='ROLE_MECANICO'
    AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email='mecanico1.demo@oficina.test');

-- Diego Ribeiro / ROLE_MECANICO
INSERT INTO usuario (email,senha,ativo,bloqueado,funcao_id)
SELECT 'mecanico2.demo@oficina.test', '$2y$12$9Y5G2LC0r4IlJQamIfSmNOrXBm9mguDOdznKJv0pQGR.kidaHslOy', 1, 0, f.id
  FROM funcao f WHERE f.nome='ROLE_MECANICO'
    AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email='mecanico2.demo@oficina.test');

-- Larissa Mendes / ROLE_ASSISTENTE_ADMINISTRATIVO
INSERT INTO usuario (email,senha,ativo,bloqueado,funcao_id)
SELECT 'assistente1.demo@oficina.test', '$2y$12$3QZxpSzmKiIa90O6vtxx6.aMUV61iZac8f3NbWmWR4chscjhFni4G', 1, 0, f.id
  FROM funcao f WHERE f.nome='ROLE_ASSISTENTE_ADMINISTRATIVO'
    AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email='assistente1.demo@oficina.test');

-- Camila Duarte / ROLE_ASSISTENTE_ADMINISTRATIVO
INSERT INTO usuario (email,senha,ativo,bloqueado,funcao_id)
SELECT 'assistente2.demo@oficina.test', '$2y$12$7bEjRY6J0oVOHCRVIlvZ1O2tHH8UsAODxinwLNkfsazB5sC2.XPUu', 1, 0, f.id
  FROM funcao f WHERE f.nome='ROLE_ASSISTENTE_ADMINISTRATIVO'
    AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email='assistente2.demo@oficina.test');

-- 2. Funcionarios vinculados aos usuarios acima
-- salario=0 somente para satisfazer coluna historica NOT NULL; nao representa valor real.
INSERT INTO funcionario (nome,cpf,telefone,email,cargo,salario,status,usuario_id)
SELECT 'Marina Farias', '123.456.789-09', '(11) 90000-1001', 'gerente.demo@oficina.test', 'Gerente', 0.00, 'ATIVO', u.id
  FROM usuario u WHERE u.email='gerente.demo@oficina.test'
    AND NOT EXISTS (SELECT 1 FROM funcionario f WHERE f.cpf='123.456.789-09');

INSERT INTO funcionario (nome,cpf,telefone,email,cargo,salario,status,usuario_id)
SELECT 'Bruno Nogueira', '987.654.321-00', '(11) 90000-1002', 'mecanico1.demo@oficina.test', 'Mecânico', 0.00, 'ATIVO', u.id
  FROM usuario u WHERE u.email='mecanico1.demo@oficina.test'
    AND NOT EXISTS (SELECT 1 FROM funcionario f WHERE f.cpf='987.654.321-00');

INSERT INTO funcionario (nome,cpf,telefone,email,cargo,salario,status,usuario_id)
SELECT 'Diego Ribeiro', '112.233.445-17', '(11) 90000-1003', 'mecanico2.demo@oficina.test', 'Mecânico', 0.00, 'ATIVO', u.id
  FROM usuario u WHERE u.email='mecanico2.demo@oficina.test'
    AND NOT EXISTS (SELECT 1 FROM funcionario f WHERE f.cpf='112.233.445-17');

INSERT INTO funcionario (nome,cpf,telefone,email,cargo,salario,status,usuario_id)
SELECT 'Larissa Mendes', '223.344.556-28', '(11) 90000-1004', 'assistente1.demo@oficina.test', 'Assistente Administrativa', 0.00, 'ATIVO', u.id
  FROM usuario u WHERE u.email='assistente1.demo@oficina.test'
    AND NOT EXISTS (SELECT 1 FROM funcionario f WHERE f.cpf='223.344.556-28');

INSERT INTO funcionario (nome,cpf,telefone,email,cargo,salario,status,usuario_id)
SELECT 'Camila Duarte', '334.455.667-39', '(11) 90000-1005', 'assistente2.demo@oficina.test', 'Assistente Administrativa', 0.00, 'ATIVO', u.id
  FROM usuario u WHERE u.email='assistente2.demo@oficina.test'
    AND NOT EXISTS (SELECT 1 FROM funcionario f WHERE f.cpf='334.455.667-39');

-- 3. Clientes ficticios com CPFs validos matematicamente
INSERT INTO cliente (nome,cpf,telefone,email,endereco)
SELECT 'Roberto Almeida', '445.566.778-40', '(11) 98000-2001', 'roberto.demo@exemplo.test', 'Rua das Acácias, 100'
WHERE NOT EXISTS (SELECT 1 FROM cliente c WHERE c.cpf='445.566.778-40');

INSERT INTO cliente (nome,cpf,telefone,email,endereco)
SELECT 'Ana Beatriz Costa', '556.677.889-50', '(11) 98000-2002', 'ana.demo@exemplo.test', 'Rua Ipê, 202'
WHERE NOT EXISTS (SELECT 1 FROM cliente c WHERE c.cpf='556.677.889-50');

INSERT INTO cliente (nome,cpf,telefone,email,endereco)
SELECT 'Lucas Ferreira', '667.788.990-42', '(11) 98000-2003', 'lucas.demo@exemplo.test', 'Av. Brasil, 305'
WHERE NOT EXISTS (SELECT 1 FROM cliente c WHERE c.cpf='667.788.990-42');

INSERT INTO cliente (nome,cpf,telefone,email,endereco)
SELECT 'Patrícia Gomes', '778.899.001-98', '(11) 98000-2004', 'patricia.demo@exemplo.test', 'Rua das Flores, 48'
WHERE NOT EXISTS (SELECT 1 FROM cliente c WHERE c.cpf='778.899.001-98');

INSERT INTO cliente (nome,cpf,telefone,email,endereco)
SELECT 'Rafael Martins', '889.900.112-05', '(11) 98000-2005', 'rafael.demo@exemplo.test', 'Av. Central, 900'
WHERE NOT EXISTS (SELECT 1 FROM cliente c WHERE c.cpf='889.900.112-05');

INSERT INTO cliente (nome,cpf,telefone,email,endereco)
SELECT 'Juliana Carvalho', '990.011.223-70', '(11) 98000-2006', 'juliana.demo@exemplo.test', 'Rua Aurora, 71'
WHERE NOT EXISTS (SELECT 1 FROM cliente c WHERE c.cpf='990.011.223-70');

-- 4. Veiculos associados aos clientes por CPF, sem depender de IDs fixos
INSERT INTO veiculo (marca,modelo,ano,cor,placa,quilometragem,cliente_id)
SELECT 'Chevrolet', 'Onix', 2020, 'Prata', 'AAA1A11', 52000, c.id
  FROM cliente c WHERE c.cpf='445.566.778-40'
    AND NOT EXISTS (SELECT 1 FROM veiculo v WHERE v.placa='AAA1A11');

INSERT INTO veiculo (marca,modelo,ano,cor,placa,quilometragem,cliente_id)
SELECT 'Volkswagen', 'Gol', 2018, 'Branco', 'BBB2B22', 87500, c.id
  FROM cliente c WHERE c.cpf='556.677.889-50'
    AND NOT EXISTS (SELECT 1 FROM veiculo v WHERE v.placa='BBB2B22');

INSERT INTO veiculo (marca,modelo,ano,cor,placa,quilometragem,cliente_id)
SELECT 'Fiat', 'Argo', 2021, 'Vermelho', 'CCC3C33', 43000, c.id
  FROM cliente c WHERE c.cpf='667.788.990-42'
    AND NOT EXISTS (SELECT 1 FROM veiculo v WHERE v.placa='CCC3C33');

INSERT INTO veiculo (marca,modelo,ano,cor,placa,quilometragem,cliente_id)
SELECT 'Hyundai', 'HB20', 2019, 'Preto', 'DDD4D44', 79000, c.id
  FROM cliente c WHERE c.cpf='778.899.001-98'
    AND NOT EXISTS (SELECT 1 FROM veiculo v WHERE v.placa='DDD4D44');

INSERT INTO veiculo (marca,modelo,ano,cor,placa,quilometragem,cliente_id)
SELECT 'Toyota', 'Corolla', 2022, 'Cinza', 'EEE5E55', 27500, c.id
  FROM cliente c WHERE c.cpf='889.900.112-05'
    AND NOT EXISTS (SELECT 1 FROM veiculo v WHERE v.placa='EEE5E55');

INSERT INTO veiculo (marca,modelo,ano,cor,placa,quilometragem,cliente_id)
SELECT 'Renault', 'Sandero', 2017, 'Azul', 'FFF6F66', 112000, c.id
  FROM cliente c WHERE c.cpf='990.011.223-70'
    AND NOT EXISTS (SELECT 1 FROM veiculo v WHERE v.placa='FFF6F66');

INSERT INTO veiculo (marca,modelo,ano,cor,placa,quilometragem,cliente_id)
SELECT 'Honda', 'Fit', 2016, 'Prata', 'GGG7G77', 136000, c.id
  FROM cliente c WHERE c.cpf='445.566.778-40'
    AND NOT EXISTS (SELECT 1 FROM veiculo v WHERE v.placa='GGG7G77');

INSERT INTO veiculo (marca,modelo,ano,cor,placa,quilometragem,cliente_id)
SELECT 'Chevrolet', 'Prisma', 2015, 'Branco', 'HHH8H88', 98000, c.id
  FROM cliente c WHERE c.cpf='667.788.990-42'
    AND NOT EXISTS (SELECT 1 FROM veiculo v WHERE v.placa='HHH8H88');

-- 5. Catalogo de pecas + saldos iniciais de DEMONSTRACAO
-- A V6 registra automaticamente o evento CADASTRO para os veiculos.
-- O saldo e carregado no cadastro, e o movimento ENTRADA abaixo documenta a carga.
INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Filtro de óleo', 'Mann', 20, 45.90, 5
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Filtro de óleo');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Filtro de ar', 'Fram', 18, 59.90, 5
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Filtro de ar');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Filtro de combustível', 'Tecfil', 12, 68.00, 4
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Filtro de combustível');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Óleo de motor 5W30 1L', 'Mobil', 50, 54.90, 12
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Óleo de motor 5W30 1L');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Pastilha de freio dianteira', 'Cobreq', 10, 189.90, 4
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Pastilha de freio dianteira');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Disco de freio dianteiro', 'Fremax', 8, 259.90, 3
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Disco de freio dianteiro');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Vela de ignição', 'NGK', 28, 42.50, 8
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Vela de ignição');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Correia dentada', 'Gates', 7, 169.00, 3
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Correia dentada');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Bateria 60Ah', 'Moura', 6, 549.00, 2
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Bateria 60Ah');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Amortecedor dianteiro', 'Monroe', 5, 329.90, 2
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Amortecedor dianteiro');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Líquido de arrefecimento 1L', 'Radiex', 25, 32.00, 8
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Líquido de arrefecimento 1L');

INSERT INTO peca (nome,fabricante,quantidade,valor_unitario,estoque_minimo)
SELECT 'Palheta limpador de para-brisa', 'Bosch', 3, 89.90, 5
WHERE NOT EXISTS (SELECT 1 FROM peca p WHERE p.nome='Palheta limpador de para-brisa');

-- 6. Historico de carga inicial do estoque (sem duplicar movimentos)
INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',20,26.00,'Carga inicial demonstracao - Filtro de óleo'
  FROM peca p WHERE p.nome='Filtro de óleo'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Filtro de óleo');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',18,32.00,'Carga inicial demonstracao - Filtro de ar'
  FROM peca p WHERE p.nome='Filtro de ar'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Filtro de ar');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',12,38.00,'Carga inicial demonstracao - Filtro de combustível'
  FROM peca p WHERE p.nome='Filtro de combustível'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Filtro de combustível');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',50,31.00,'Carga inicial demonstracao - Óleo de motor 5W30 1L'
  FROM peca p WHERE p.nome='Óleo de motor 5W30 1L'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Óleo de motor 5W30 1L');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',10,110.00,'Carga inicial demonstracao - Pastilha de freio dianteira'
  FROM peca p WHERE p.nome='Pastilha de freio dianteira'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Pastilha de freio dianteira');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',8,160.00,'Carga inicial demonstracao - Disco de freio dianteiro'
  FROM peca p WHERE p.nome='Disco de freio dianteiro'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Disco de freio dianteiro');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',28,24.00,'Carga inicial demonstracao - Vela de ignição'
  FROM peca p WHERE p.nome='Vela de ignição'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Vela de ignição');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',7,91.00,'Carga inicial demonstracao - Correia dentada'
  FROM peca p WHERE p.nome='Correia dentada'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Correia dentada');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',6,390.00,'Carga inicial demonstracao - Bateria 60Ah'
  FROM peca p WHERE p.nome='Bateria 60Ah'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Bateria 60Ah');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',5,210.00,'Carga inicial demonstracao - Amortecedor dianteiro'
  FROM peca p WHERE p.nome='Amortecedor dianteiro'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Amortecedor dianteiro');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',25,17.00,'Carga inicial demonstracao - Líquido de arrefecimento 1L'
  FROM peca p WHERE p.nome='Líquido de arrefecimento 1L'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Líquido de arrefecimento 1L');

INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,custo_unitario,observacao)
SELECT p.id,'ENTRADA',3,46.00,'Carga inicial demonstracao - Palheta limpador de para-brisa'
  FROM peca p WHERE p.nome='Palheta limpador de para-brisa'
    AND NOT EXISTS (SELECT 1 FROM movimentacao_estoque m WHERE m.peca_id=p.id AND m.observacao='Carga inicial demonstracao - Palheta limpador de para-brisa');

COMMIT;

-- 7. CONFERENCIA DO POVOAMENTO
SELECT 'Clientes' AS cadastro, COUNT(*) AS total FROM cliente
UNION ALL SELECT 'Veiculos',COUNT(*) FROM veiculo
UNION ALL SELECT 'Pecas',COUNT(*) FROM peca
UNION ALL SELECT 'Funcionarios ativos',COUNT(*) FROM funcionario WHERE status='ATIVO'
UNION ALL SELECT 'Entradas de estoque',COUNT(*) FROM movimentacao_estoque WHERE tipo='ENTRADA';

SELECT f.nome, f.cargo, u.email, r.nome AS perfil, u.ativo
FROM funcionario f JOIN usuario u ON u.id=f.usuario_id JOIN funcao r ON r.id=u.funcao_id
WHERE u.email LIKE '%.demo@oficina.test' ORDER BY r.nome, f.nome;

-- Resultado esperado em uma instalacao limpa: 6 clientes, 8 veiculos, 12 pecas,
-- 5 funcionarios, 12 entradas de estoque e 1 administrador ja existente.
