CREATE TABLE execucao_servico (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 os_id BIGINT NOT NULL,
 item_orcamento_id BIGINT NOT NULL UNIQUE,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
     CHECK (status IN ('PENDENTE','EM_ANDAMENTO','CONCLUIDO')),
 iniciado_em DATETIME(6),
 concluido_em DATETIME(6),
 CONSTRAINT fk_execucao_os FOREIGN KEY (os_id) REFERENCES ordem_servico(id),
 CONSTRAINT fk_execucao_item FOREIGN KEY (item_orcamento_id) REFERENCES item_orcamento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_execucao_servico_os ON execucao_servico(os_id);
