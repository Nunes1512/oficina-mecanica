package com.oficina.repository;

import com.oficina.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByFuncaoNome(String nomeDaFuncao);

    /** Quantos administradores ainda conseguem entrar (impede bloquear o último). */
    long countByFuncaoNomeAndAtivoTrueAndBloqueadoFalse(String nomeDaFuncao);

    List<Usuario> findAllByOrderByEmail();
}
