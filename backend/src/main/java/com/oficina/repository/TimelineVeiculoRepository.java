package com.oficina.repository;

import com.oficina.model.TimelineVeiculo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineVeiculoRepository extends JpaRepository<TimelineVeiculo, Long> {

    List<TimelineVeiculo> findByVeiculoIdOrderByDataAscIdAsc(Long veiculoId);
}
