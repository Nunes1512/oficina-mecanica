package com.oficina.repository;

import com.oficina.model.OrdemServico;
import com.oficina.model.StatusOs;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
    List<OrdemServico> findAllByOrderByDataAberturaDesc();
    List<OrdemServico> findByFuncionarioIdOrderByDataAberturaDesc(Long funcionarioId);
    List<OrdemServico> findByStatusOrderByDataAberturaDesc(StatusOs status);
    List<OrdemServico> findByVeiculoIdOrderByDataAberturaDesc(Long veiculoId);
    List<OrdemServico> findTop5ByOrderByDataAberturaDesc();
    long countByVeiculoId(Long veiculoId);

    /** Serializa as operações de execução e de consumo para impedir dupla baixa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServico o where o.id = :id")
    Optional<OrdemServico> bloquearParaExecucao(@Param("id") Long id);

    @Query("select o from OrdemServico o where o.status not in :finais "
            + "and o.veiculo.previsaoEntrega < :agora order by o.veiculo.previsaoEntrega")
    List<OrdemServico> findAtrasadas(@Param("finais") Collection<StatusOs> finais,
                                     @Param("agora") LocalDateTime agora);
}
