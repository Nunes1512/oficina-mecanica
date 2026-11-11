package com.oficina.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Pagamento integral de uma OS, sem sobrescrita posterior e sem cobrança duplicada. */
@Entity
@Table(name = "pagamento_os")
@Getter @Setter @NoArgsConstructor
public class PagamentoOs {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false, unique = true)
    private OrdemServico os;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 25)
    private FormaPagamentoOs formaPagamento;

    @Column(name = "pago_em", nullable = false)
    private LocalDateTime pagoEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_usuario_id", nullable = false)
    private Usuario registradoPorUsuario;

    @Column(length = 500)
    private String observacao;

    @Column(name = "comprovante_nome", length = 180)
    private String comprovanteNome;

    @Column(name = "comprovante_mime", length = 40)
    private String comprovanteMime;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "comprovante_dados", columnDefinition = "bytea")
    private byte[] comprovanteDados;
}
