package com.oficina.repository;

import com.oficina.model.Funcionario;
import com.oficina.model.StatusFuncionario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    /** Funcionário ligado a um usuário de login (null se o usuário não for funcionário). */
    Optional<Funcionario> findByUsuarioId(Long usuarioId);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    List<Funcionario> findAllByOrderByNome();

    List<Funcionario> findByStatusOrderByNome(StatusFuncionario status);

    /** Funcionários que ainda não têm usuário de login. */
    List<Funcionario> findByUsuarioIsNullOrderByNome();
}
