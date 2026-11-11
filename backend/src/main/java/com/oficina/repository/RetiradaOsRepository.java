package com.oficina.repository;

import com.oficina.model.RetiradaOs;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetiradaOsRepository extends JpaRepository<RetiradaOs, Long> {
    Optional<RetiradaOs> findByOsId(Long osId);
    boolean existsByOsId(Long osId);
}
