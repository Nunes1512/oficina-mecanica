-- Evento real de entrada é a abertura de OS, não o cadastro do veículo.
-- Na versão MariaDB a V1 já admite CADASTRO e V6 registra o cadastro corretamente.
DELIMITER $$
CREATE TRIGGER trg_os_timeline_abertura AFTER INSERT ON ordem_servico
FOR EACH ROW
BEGIN
 INSERT INTO timeline_veiculo (veiculo_id,evento,descricao)
 VALUES (NEW.veiculo_id,'ENTRADA',CONCAT('Entrada na oficina - OS ',NEW.numero));
END $$
DELIMITER ;

-- Ajuste de preços sem mudança de quantidade já está tratado em V9.
