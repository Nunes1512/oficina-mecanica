-- Criação das funções e tabelas de orçamento em versões REV1, REV2...
INSERT IGNORE INTO funcao (nome) VALUES ('ROLE_MECANICO'),('ROLE_ASSISTENTE_ADMINISTRATIVO');

-- status e CHECK já foram criados no formato final em V1, porque este pacote é só para banco vazio.
CREATE TABLE orcamento (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 os_id BIGINT NOT NULL,
 versao INT NOT NULL CHECK (versao > 0),
 aprovado_gerente BOOLEAN NOT NULL DEFAULT FALSE,
 decisao_cliente VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
  CHECK (decisao_cliente IN ('PENDENTE','APROVADO','ALTERACAO','RECUSADO')),
 contato_por_usuario_id BIGINT,
 registrado_por_usuario_id BIGINT,
 data_contato DATETIME(6),
 observacao_cliente VARCHAR(500),
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT uk_orcamento_os_versao UNIQUE (os_id,versao),
 CONSTRAINT fk_orcamento_os FOREIGN KEY (os_id) REFERENCES ordem_servico(id),
 CONSTRAINT fk_orcamento_contato FOREIGN KEY (contato_por_usuario_id) REFERENCES usuario(id),
 CONSTRAINT fk_orcamento_registrador FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE item_orcamento (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 orcamento_id BIGINT NOT NULL,
 tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('SERVICO','PECA')),
 descricao VARCHAR(255) NOT NULL,
 peca_id BIGINT,
 quantidade INT NOT NULL CHECK (quantidade > 0),
 valor_unitario DECIMAL(10,2) CHECK (valor_unitario >= 0),
 CONSTRAINT chk_item_orcamento_peca CHECK (
      (tipo='PECA' AND peca_id IS NOT NULL) OR
      (tipo='SERVICO' AND peca_id IS NULL)
 ),
 CONSTRAINT fk_item_orcamento_orc FOREIGN KEY (orcamento_id) REFERENCES orcamento(id),
 CONSTRAINT fk_item_orcamento_peca FOREIGN KEY (peca_id) REFERENCES peca(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_orcamento_os ON orcamento(os_id,versao);
CREATE INDEX idx_item_orcamento_id ON item_orcamento(orcamento_id);
