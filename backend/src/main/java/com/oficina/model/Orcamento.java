package com.oficina.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Versao do orcamento. Respostas ja registradas nunca devem ser reescritas. */
@Entity
@Table(name = "orcamento")
@Getter @Setter @NoArgsConstructor
public class Orcamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "os_id", nullable = false)
    private OrdemServico os;

    @Column(nullable = false)
    private Integer versao;

    @Column(name = "aprovado_gerente", nullable = false)
    private boolean aprovadoGerente;

    @Enumerated(EnumType.STRING)
    @Column(name = "decisao_cliente", nullable = false, length = 20)
    private DecisaoCliente decisaoCliente = DecisaoCliente.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contato_por_usuario_id")
    private Usuario contatoPorUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_usuario_id")
    private Usuario registradoPorUsuario;

    @Column(name = "data_contato")
    private LocalDateTime dataContato;

    @Column(name = "observacao_cliente", length = 500)
    private String observacaoCliente;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private LocalDateTime criadoEm;
}
