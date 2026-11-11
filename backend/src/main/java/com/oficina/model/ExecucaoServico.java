package com.oficina.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro do progresso de um serviço pertencente ao orçamento aprovado. */
@Entity
@Table(name = "execucao_servico")
@Getter @Setter @NoArgsConstructor
public class ExecucaoServico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false)
    private OrdemServico os;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_orcamento_id", nullable = false, unique = true)
    private ItemOrcamento itemOrcamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusExecucaoServico status = StatusExecucaoServico.PENDENTE;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;
}
