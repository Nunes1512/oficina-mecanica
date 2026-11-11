package com.oficina.repository;

import com.oficina.model.Veiculo;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    boolean existsByPlaca(String placa);
    boolean existsByPlacaAndIdNot(String placa, Long id);
    Optional<Veiculo> findByPlacaIgnoreCase(String placa);
    long countByClienteId(Long clienteId);
    List<Veiculo> findAllByOrderByDataEntradaDesc();
    List<Veiculo> findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCaseOrderByDataEntradaDesc(String placa, String modelo);
    List<Veiculo> findByClienteIdOrderByDataEntradaDesc(Long clienteId);

    /** Bloqueio pessimista para que duas recepções não abram OS do mesmo veículo simultaneamente. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Veiculo v where v.id = :id")
    Optional<Veiculo> bloquearParaAbertura(@Param("id") Long id);
}
