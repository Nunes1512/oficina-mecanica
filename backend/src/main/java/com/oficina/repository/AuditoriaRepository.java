package com.oficina.repository;

import com.oficina.model.Auditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findTop100ByOrderByDataDesc();
}
