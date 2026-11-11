-- Triggers portados do PostgreSQL. Antes de cada UPDATE, o total da OS é recalculado.
DELIMITER $$
CREATE TRIGGER trg_os_calcula_total_ins BEFORE INSERT ON ordem_servico
FOR EACH ROW
BEGIN
 SET NEW.valor_total = COALESCE(NEW.valor_mao_obra,0) + COALESCE(NEW.valor_pecas,0);
END $$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_os_calcula_total_upd BEFORE UPDATE ON ordem_servico
FOR EACH ROW
BEGIN
 SET NEW.valor_total = COALESCE(NEW.valor_mao_obra,0) + COALESCE(NEW.valor_pecas,0);
END $$
DELIMITER ;

-- Inserir item efetivamente consumido desconta estoque, registra movimento e soma a OS.
DELIMITER $$
CREATE TRIGGER trg_item_os_baixa_estoque AFTER INSERT ON item_os
FOR EACH ROW
BEGIN
 DECLARE v_saldo INT DEFAULT NULL;
 SELECT quantidade INTO v_saldo FROM peca WHERE id = NEW.peca_id FOR UPDATE;
 IF v_saldo IS NULL OR v_saldo < NEW.quantidade THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estoque insuficiente para item da OS';
 END IF;
 UPDATE peca SET quantidade = quantidade - NEW.quantidade WHERE id = NEW.peca_id;
 INSERT INTO movimentacao_estoque (peca_id, tipo, quantidade, os_id, observacao)
 VALUES (NEW.peca_id,'SAIDA',NEW.quantidade,NEW.os_id,'Utilizada na OS');
 UPDATE ordem_servico
    SET valor_pecas = valor_pecas + (NEW.quantidade * NEW.valor_unitario)
  WHERE id = NEW.os_id;
END $$
DELIMITER ;
