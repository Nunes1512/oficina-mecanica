-- A procedure não retorna linhas nem controla transações: pode ser chamada por triggers.
DELIMITER $$
CREATE PROCEDURE sp_notificar_gestao(IN p_tipo VARCHAR(30), IN p_msg VARCHAR(255))
BEGIN
 INSERT INTO notificacao (usuario_id, tipo, mensagem)
 SELECT u.id, p_tipo, p_msg
   FROM usuario u JOIN funcao r ON r.id = u.funcao_id
  WHERE u.ativo = TRUE AND u.bloqueado = FALSE
    AND r.nome IN ('ROLE_GERENTE','ROLE_ADMIN_MASTER');
END $$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_peca_estoque_baixo AFTER UPDATE ON peca
FOR EACH ROW
BEGIN
 IF OLD.quantidade > NEW.estoque_minimo AND NEW.quantidade <= NEW.estoque_minimo THEN
    CALL sp_notificar_gestao('ESTOQUE_BAIXO',
         CONCAT('Estoque baixo: ',NEW.nome,' (',NEW.quantidade,' un., mínimo ',NEW.estoque_minimo,')'));
 END IF;
END $$
DELIMITER ;

DELIMITER $$
CREATE TRIGGER trg_solicitacao_notifica AFTER INSERT ON solicitacao_peca
FOR EACH ROW
BEGIN
 CALL sp_notificar_gestao('SOLICITACAO_PENDENTE',
      CONCAT('Nova solicitação de peça pendente (#',NEW.id,')'));
END $$
DELIMITER ;
