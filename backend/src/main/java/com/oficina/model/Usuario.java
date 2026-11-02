package com.oficina.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Credenciais de login (tabela usuario). A senha é sempre um hash BCrypt. */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "senha", nullable = false, length = 100)
    private String senhaHash;

    /** Exclusão lógica: false = usuário desativado, sem sumir do banco. */
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean bloqueado = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "funcao_id", nullable = false)
    private Funcao funcao;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private LocalDateTime criadoEm;
}
