package com.oficina.repository;

import com.oficina.model.Veiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    boolean existsByPlaca(String placa);
    boolean existsByPlacaAndIdNot(String placa, Long id);
    Optional<Veiculo> findByPlacaIgnoreCase(String placa);
    long countByClienteId(Long clienteId);
    List<Veiculo> findAllByOrderByDataEntradaDesc();
    List<Veiculo> findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCaseOrderByDataEntradaDesc(String placa, String modelo);
    List<Veiculo> findByClienteIdOrderByDataEntradaDesc(Long clienteId);
}
