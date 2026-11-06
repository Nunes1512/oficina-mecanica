package com.oficina.repository;

import com.oficina.model.MovimentacaoEstoque;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    List<MovimentacaoEstoque> findByPecaIdOrderByDataDescIdDesc(Long pecaId);

    boolean existsByPecaId(Long pecaId);
}
