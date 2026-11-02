-- Ao remover um item da OS, devolve as peças ao estoque, registra o estorno no histórico
-- e recalcula o valor das peças da OS. Corrige a falha do V4, que só tratava o INSERT
CREATE OR REPLACE FUNCTION fn_item_os_estorno_estoque() RETURNS trigger AS $$
BEGIN
    UPDATE peca SET quantidade = quantidade + OLD.quantidade WHERE id = OLD.peca_id;

    INSERT INTO movimentacao_estoque (peca_id, tipo, quantidade, observacao)
    VALUES (OLD.peca_id, 'ENTRADA', OLD.quantidade, 'Estorno: item removido da OS ' || OLD.os_id);

    UPDATE ordem_servico
       SET valor_pecas = (SELECT COALESCE(SUM(quantidade * valor_unitario), 0)
                            FROM item_os WHERE os_id = OLD.os_id)
     WHERE id = OLD.os_id;

    RETURN OLD;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_item_os_estorno_estoque
AFTER DELETE ON item_os
FOR EACH ROW EXECUTE FUNCTION fn_item_os_estorno_estoque();

-- Ao alterar a quantidade de um item da OS, ajusta o estoque pela diferença.
-- Trocar a peça ou a OS do item é proibido: o correto é remover e adicionar de novo
CREATE OR REPLACE FUNCTION fn_item_os_ajusta_estoque() RETURNS trigger AS $$
DECLARE
    v_saldo INT;
    v_dif   INT;
BEGIN
    IF NEW.peca_id <> OLD.peca_id OR NEW.os_id <> OLD.os_id THEN
        RAISE EXCEPTION 'Não é permitido trocar a peça ou a OS de um item: remova e adicione novamente';
    END IF;

    v_dif := NEW.quantidade - OLD.quantidade;
    IF v_dif = 0 THEN
        RETURN NEW;
    END IF;

    SELECT quantidade INTO v_saldo FROM peca WHERE id = NEW.peca_id FOR UPDATE;

    IF v_dif > 0 AND v_saldo < v_dif THEN
        RAISE EXCEPTION 'Estoque insuficiente para a peça % (saldo: %, necessário a mais: %)',
            NEW.peca_id, v_saldo, v_dif;
    END IF;

    UPDATE peca SET quantidade = quantidade - v_dif WHERE id = NEW.peca_id;

    INSERT INTO movimentacao_estoque (peca_id, tipo, quantidade, os_id, observacao)
    VALUES (NEW.peca_id, CASE WHEN v_dif > 0 THEN 'SAIDA' ELSE 'ENTRADA' END,
            ABS(v_dif), NEW.os_id, 'Ajuste de quantidade do item na OS');

    UPDATE ordem_servico
       SET valor_pecas = (SELECT COALESCE(SUM(quantidade * valor_unitario), 0)
                            FROM item_os WHERE os_id = NEW.os_id)
     WHERE id = NEW.os_id;

    RETURN NEW;
END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_item_os_ajusta_estoque
AFTER UPDATE OF quantidade, peca_id, os_id, valor_unitario ON item_os
FOR EACH ROW EXECUTE FUNCTION fn_item_os_ajusta_estoque();
