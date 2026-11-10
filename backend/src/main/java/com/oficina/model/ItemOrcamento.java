package com.oficina.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Item planejado, ainda SEM baixa de estoque. */
@Entity
@Table(name = "item_orcamento")
@Getter @Setter @NoArgsConstructor
public class ItemOrcamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItemOrcamento tipo;

    @Column(nullable = false, length = 255)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id")
    private Peca peca;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "valor_unitario", precision = 10, scale = 2)
    private BigDecimal valorUnitario;
}
