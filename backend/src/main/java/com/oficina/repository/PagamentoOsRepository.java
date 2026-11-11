package com.oficina.repository;

import com.oficina.model.PagamentoOs;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoOsRepository extends JpaRepository<PagamentoOs, Long> {
    Optional<PagamentoOs> findByOsId(Long osId);
    boolean existsByOsId(Long osId);
}
