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
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/** Cadastro permanente. Datas no veículo são somente o último atendimento; histórico está na OS/retirada. */
@Entity
@Table(name = "veiculo")
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
public class Veiculo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String marca;
    @Column(nullable = false, length = 60)
    private String modelo;
    private Integer ano;
    @Column(length = 30)
    private String cor;
    @Column(nullable = false, unique = true, length = 8)
    private String placa;
    private Integer quilometragem;
    /** Data de cadastro até a primeira OS; depois, data da abertura da última visita. */
    @Column(name = "data_entrada", nullable = false)
    private LocalDateTime dataEntrada;
    @Column(name = "previsao_entrega")
    private LocalDateTime previsaoEntrega;
    /** Precisa ser gravável para limpar a entrega antiga ao abrir nova visita. */
    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVeiculo status = StatusVeiculo.AGUARDANDO_ANALISE;
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    public long getDiasNaOficina() {
        if (dataEntrada == null) return 0;
        LocalDateTime fim = dataEntrega == null ? LocalDateTime.now() : dataEntrega;
        return Math.max(0, ChronoUnit.DAYS.between(dataEntrada, fim));
    }
}
