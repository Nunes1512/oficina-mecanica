package com.oficina.repository;

import com.oficina.model.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, Long id);
    List<Cliente> findAllByOrderByNome();
    List<Cliente> findByNomeContainingIgnoreCaseOrCpfContainingOrderByNome(String nome, String cpf);

    /** Quantos clientes foram cadastrados nos 12 meses recentes com cadastros. */
    @Query(value = "SELECT DATE_FORMAT(MIN(criado_em), '%m/%Y') AS mes, COUNT(*) AS total "
          + "FROM cliente GROUP BY YEAR(criado_em), MONTH(criado_em) "
          + "ORDER BY YEAR(criado_em) DESC, MONTH(criado_em) DESC LIMIT 12", nativeQuery = true)
    List<ClientesPorMes> clientesPorMes();

    interface ClientesPorMes {
        String getMes();
        Long getTotal();
    }
}
