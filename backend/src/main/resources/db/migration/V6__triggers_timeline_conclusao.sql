-- Cadastro e chegada física são eventos diferentes; abertura da OS cria ENTRADA na V13.
DELIMITER $$
CREATE TRIGGER trg_veiculo_timeline_insert AFTER INSERT ON veiculo
FOR EACH ROW
BEGIN
 INSERT INTO timeline_veiculo (veiculo_id, evento, descricao, data)
 VALUES (NEW.id, 'CADASTRO', 'Veículo cadastrado no sistema', NEW.data_entrada);
END $$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_veiculo_timeline_status BEFORE UPDATE ON veiculo
FOR EACH ROW
BEGIN
 DECLARE v_evento VARCHAR(30);
 IF NOT (OLD.status <=> NEW.status) THEN
   SET v_evento = CASE NEW.status
     WHEN 'EM_MANUTENCAO' THEN 'INICIO_CONSERTO'
     WHEN 'CONCLUIDO' THEN 'CONCLUSAO'
     WHEN 'ENTREGUE' THEN 'ENTREGA'
     ELSE 'MUDANCA_STATUS' END;
   INSERT INTO timeline_veiculo (veiculo_id, evento, descricao)
   VALUES (NEW.id, v_evento, CONCAT('Status alterado de ',OLD.status,' para ',NEW.status));
   IF NEW.status = 'ENTREGUE' AND NEW.data_entrega IS NULL THEN
      SET NEW.data_entrega = CURRENT_TIMESTAMP(6);
   END IF;
   IF NEW.status = 'AGUARDANDO_PECAS' THEN
      CALL sp_notificar_gestao('VEICULO_AGUARDANDO_PECA',
           CONCAT('Veículo ',NEW.placa,' aguardando peças'));
   END IF;
 END IF;
END $$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_os_concluida BEFORE UPDATE ON ordem_servico
FOR EACH ROW
BEGIN
 IF NEW.status = 'CONCLUIDA' AND OLD.status <> 'CONCLUIDA' THEN
    IF NEW.data_conclusao IS NULL THEN
       SET NEW.data_conclusao = CURRENT_TIMESTAMP(6);
    END IF;
    CALL sp_notificar_gestao('SERVICO_CONCLUIDO',CONCAT('OS ',NEW.numero,' concluída'));
 END IF;
END $$
DELIMITER ;
