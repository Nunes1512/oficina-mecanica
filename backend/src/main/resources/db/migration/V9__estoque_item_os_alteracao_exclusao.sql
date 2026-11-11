-- No MariaDB, recalcular por delta evita consultar a tabela item_os de dentro do próprio trigger.
DELIMITER $$
CREATE TRIGGER trg_item_os_estorno_estoque AFTER DELETE ON item_os
FOR EACH ROW
BEGIN
 UPDATE peca SET quantidade = quantidade + OLD.quantidade WHERE id = OLD.peca_id;
 INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,os_id,observacao)
 VALUES (OLD.peca_id,'ENTRADA',OLD.quantidade,OLD.os_id,
         CONCAT('Estorno: item removido da OS ',OLD.os_id));
 UPDATE ordem_servico
    SET valor_pecas = valor_pecas - (OLD.quantidade * OLD.valor_unitario)
  WHERE id = OLD.os_id;
END $$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_item_os_ajusta_estoque AFTER UPDATE ON item_os
FOR EACH ROW
BEGIN
 DECLARE v_dif INT DEFAULT 0;
 DECLARE v_saldo INT DEFAULT NULL;
 IF NEW.peca_id <> OLD.peca_id OR NEW.os_id <> OLD.os_id THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Não é permitido trocar peça ou OS de um item';
 END IF;
 SET v_dif = NEW.quantidade - OLD.quantidade;
 IF v_dif <> 0 THEN
    SELECT quantidade INTO v_saldo FROM peca WHERE id = NEW.peca_id FOR UPDATE;
    IF v_saldo IS NULL OR (v_dif > 0 AND v_saldo < v_dif) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Estoque insuficiente para ajuste da OS';
    END IF;
    UPDATE peca SET quantidade = quantidade - v_dif WHERE id = NEW.peca_id;
    INSERT INTO movimentacao_estoque (peca_id,tipo,quantidade,os_id,observacao)
    VALUES (NEW.peca_id,IF(v_dif > 0,'SAIDA','ENTRADA'),ABS(v_dif),NEW.os_id,
            'Ajuste de quantidade do item na OS');
 END IF;
 -- Corrige também alteração isolada de valor_unitario (QA-009).
 UPDATE ordem_servico
    SET valor_pecas = valor_pecas + (NEW.quantidade * NEW.valor_unitario)
                                     - (OLD.quantidade * OLD.valor_unitario)
  WHERE id = NEW.os_id;
END $$
DELIMITER ;
