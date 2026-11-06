package com.oficina.repository;

import com.oficina.model.OrdemServico;
import com.oficina.model.StatusOs;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findAllByOrderByDataAberturaDesc();

    List<OrdemServico> findByFuncionarioIdOrderByDataAberturaDesc(Long funcionarioId);

    List<OrdemServico> findByStatusOrderByDataAberturaDesc(StatusOs status);

    List<OrdemServico> findByVeiculoIdOrderByDataAberturaDesc(Long veiculoId);

    List<OrdemServico> findTop5ByOrderByDataAberturaDesc();

    long countByVeiculoId(Long veiculoId);

    /** OS que ainda não terminaram e cujo veículo passou da previsão de entrega. */
    @Query("select o from OrdemServico o where o.status not in :finais "
            + "and o.veiculo.previsaoEntrega < :agora order by o.veiculo.previsaoEntrega")
    List<OrdemServico> findAtrasadas(@Param("finais") Collection<StatusOs> finais,
                                     @Param("agora") LocalDateTime agora);
}
