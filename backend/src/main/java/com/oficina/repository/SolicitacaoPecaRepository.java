package com.oficina.repository;

import com.oficina.model.SolicitacaoPeca;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoPecaRepository extends JpaRepository<SolicitacaoPeca, Long> {

    List<SolicitacaoPeca> findAllByOrderByDataDesc();

    List<SolicitacaoPeca> findByFuncionarioIdOrderByDataDesc(Long funcionarioId);

    boolean existsByPecaId(Long pecaId);
}
