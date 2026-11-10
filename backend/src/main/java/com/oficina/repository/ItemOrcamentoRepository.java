package com.oficina.repository;

import com.oficina.model.ItemOrcamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {
    List<ItemOrcamento> findByOrcamentoIdOrderByIdAsc(Long orcamentoId);
}
