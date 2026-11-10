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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/** Ordem de serviço. Valores consolidados e data de conclusão vêm de triggers existentes. */
@Entity
@Table(name = "ordem_servico")
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
public class OrdemServico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @ManyToOne
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String problema;

    @Column(columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "valor_mao_obra", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMaoObra = BigDecimal.ZERO;

    @Column(name = "valor_pecas", insertable = false, updatable = false)
    private BigDecimal valorPecas;

    @Column(name = "valor_total", insertable = false, updatable = false)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusOs status = StatusOs.AGUARDANDO_AVALIACAO;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_conclusao", insertable = false, updatable = false)
    private LocalDateTime dataConclusao;
}
