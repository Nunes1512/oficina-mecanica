-- O comprovante usa LONGBLOB no MariaDB; a entidade PagamentoOs foi adaptada no pacote.
CREATE TABLE pagamento_os (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 os_id BIGINT NOT NULL UNIQUE,
 valor DECIMAL(10,2) NOT NULL CHECK (valor >= 0),
 forma_pagamento VARCHAR(25) NOT NULL CHECK
    (forma_pagamento IN ('DINHEIRO','PIX','CARTAO_DEBITO','CARTAO_CREDITO','TRANSFERENCIA')),
 pago_em DATETIME(6) NOT NULL,
 registrado_por_usuario_id BIGINT NOT NULL,
 observacao VARCHAR(500),
 comprovante_nome VARCHAR(180),
 comprovante_mime VARCHAR(40),
 comprovante_dados LONGBLOB,
 CONSTRAINT chk_pagamento_comprovante_coerente CHECK (
    (comprovante_nome IS NULL AND comprovante_mime IS NULL AND comprovante_dados IS NULL) OR
    (comprovante_nome IS NOT NULL AND comprovante_mime IS NOT NULL AND comprovante_dados IS NOT NULL)
 ),
 CONSTRAINT fk_pagamento_os FOREIGN KEY (os_id) REFERENCES ordem_servico(id),
 CONSTRAINT fk_pagamento_usuario FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE retirada_os (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 os_id BIGINT NOT NULL UNIQUE,
 nome_retirante VARCHAR(120) NOT NULL,
 retirado_em DATETIME(6) NOT NULL,
 registrado_por_usuario_id BIGINT NOT NULL,
 observacao VARCHAR(500),
 CONSTRAINT fk_retirada_os FOREIGN KEY (os_id) REFERENCES ordem_servico(id),
 CONSTRAINT fk_retirada_usuario FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
