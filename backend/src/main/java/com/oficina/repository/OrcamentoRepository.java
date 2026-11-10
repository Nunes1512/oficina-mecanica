package com.oficina.repository;

import com.oficina.model.Orcamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
    Optional<Orcamento> findTopByOsIdOrderByVersaoDesc(Long osId);
    List<Orcamento> findByOsIdOrderByVersaoDesc(Long osId);
}
