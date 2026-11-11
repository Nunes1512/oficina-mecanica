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

    /** Leva em conta a OS moderna ainda em pagamento; CONCLUIDA antiga sem orçamento é histórica. */
    @Query("select case when count(o) > 0 then true else false end from OrdemServico o "
          + "where o.veiculo.id = :veiculoId and o.status not in :finais "
          + "and (o.status <> :concluida or exists "
          + "(select oc.id from Orcamento oc where oc.os = o))")
    boolean existeAtivaParaVeiculo(@Param("veiculoId") Long veiculoId,
            @Param("finais") Collection<StatusOs> finais,
            @Param("concluida") StatusOs concluida);

    /** Legado CONCLUIDA sem orçamento aprovado não pode avançar ao pagamento novo. */
    @Query("select case when count(oc) > 0 then true else false end from Orcamento oc "
          + "where oc.os.id = :osId and oc.aprovadoGerente = true "
          + "and oc.decisaoCliente = :decisao "
          + "and oc.versao = (select max(ultimo.versao) from Orcamento ultimo where ultimo.os.id = :osId)")
    boolean temOrcamentoAprovado(@Param("osId") Long osId,
            @Param("decisao") com.oficina.model.DecisaoCliente decisao);

    /** Serializa as operações de execução e de consumo para impedir dupla baixa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdemServico o where o.id = :id")
    Optional<OrdemServico> bloquearParaExecucao(@Param("id") Long id);

    @Query("select o from OrdemServico o where o.status not in :finais "
            + "and o.veiculo.previsaoEntrega < :agora order by o.veiculo.previsaoEntrega")
    List<OrdemServico> findAtrasadas(@Param("finais") Collection<StatusOs> finais,
                                     @Param("agora") LocalDateTime agora);
}
