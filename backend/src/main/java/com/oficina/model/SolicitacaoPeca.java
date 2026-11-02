package com.oficina.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pedido de peça feito por um funcionário e decidido pelo gerente (tabela solicitacao_peca). */
@Entity
@Table(name = "solicitacao_peca")
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(length = 255)
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime data;
}
