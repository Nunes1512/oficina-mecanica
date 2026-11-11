package com.oficina.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Confirmação única da saída do veículo, vinculada à OS e à pessoa que registrou. */
@Entity
@Table(name = "retirada_os")
@Getter @Setter @NoArgsConstructor
public class RetiradaOs {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false, unique = true)
    private OrdemServico os;

    @Column(name = "nome_retirante", nullable = false, length = 120)
    private String nomeRetirante;

    @Column(name = "retirado_em", nullable = false)
    private LocalDateTime retiradoEm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_usuario_id", nullable = false)
    private Usuario registradoPorUsuario;

    @Column(length = 500)
    private String observacao;
}
