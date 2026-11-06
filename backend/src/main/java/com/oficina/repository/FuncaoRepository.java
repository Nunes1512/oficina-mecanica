package com.oficina.repository;

import com.oficina.model.Funcao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncaoRepository extends JpaRepository<Funcao, Long> {

    Optional<Funcao> findByNome(String nome);

    /** Perfis que o administrador pode atribuir (todos, menos o próprio ADMIN_MASTER). */
    List<Funcao> findByNomeNotOrderByNome(String nome);
}
