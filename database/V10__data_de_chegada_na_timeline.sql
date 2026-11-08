-- A linha do tempo passa a usar a data de chegada informada no cadastro do veículo,
-- e não o momento em que o registro foi gravado
CREATE OR REPLACE FUNCTION fn_veiculo_timeline_insert() RETURNS trigger AS $$
BEGIN
    INSERT INTO timeline_veiculo (veiculo_id, evento, descricao, data)
    VALUES (NEW.id, 'ENTRADA', 'Veículo ' || NEW.placa || ' deu entrada na oficina', NEW.data_entrada);
    RETURN NEW;
END $$ LANGUAGE plpgsql;

-- Se a data de chegada for corrigida depois, o evento de entrada acompanha
CREATE OR REPLACE FUNCTION fn_veiculo_ajusta_timeline_entrada() RETURNS trigger AS $$
BEGIN
    UPDATE timeline_veiculo
       SET data = NEW.data_entrada
     WHERE veiculo_id = NEW.id AND evento = 'ENTRADA';
    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_veiculo_ajusta_timeline_entrada
AFTER UPDATE OF data_entrada ON veiculo
FOR EACH ROW WHEN (OLD.data_entrada IS DISTINCT FROM NEW.data_entrada)
EXECUTE FUNCTION fn_veiculo_ajusta_timeline_entrada();

-- Acelera a fila de atendimento, ordenada por data de chegada
CREATE INDEX idx_veiculo_data_entrada ON veiculo(data_entrada);
