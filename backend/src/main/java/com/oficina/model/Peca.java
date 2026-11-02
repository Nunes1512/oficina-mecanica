package com.oficina.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Peça do estoque (tabela peca). O saldo só muda por entrada de estoque ou uso em OS. */
@Entity
@Table(name = "peca")
@Getter
@Setter
@NoArgsConstructor
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 80)
    private String fabricante;

    /** Somente leitura: alterada pelo banco (triggers) e pela entrada de estoque, nunca pelo formulário. */
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer quantidade;

    /** Preço de venda. */
    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo = 0;
}
