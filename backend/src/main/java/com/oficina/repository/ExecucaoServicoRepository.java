package com.oficina.repository;

import com.oficina.model.ExecucaoServico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecucaoServicoRepository extends JpaRepository<ExecucaoServico, Long> {
    List<ExecucaoServico> findByOsIdOrderById(Long osId);
    Optional<ExecucaoServico> findByOsIdAndItemOrcamentoId(Long osId, Long itemOrcamentoId);
}
