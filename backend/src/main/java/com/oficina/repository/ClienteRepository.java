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

    /** Quantos clientes foram cadastrados em cada mês (últimos 12 meses com cadastro). */
    @Query(value = "SELECT to_char(date_trunc('month', criado_em), 'MM/YYYY') AS mes, COUNT(*) AS total "
            + "FROM cliente GROUP BY date_trunc('month', criado_em) "
            + "ORDER BY date_trunc('month', criado_em) DESC LIMIT 12", nativeQuery = true)
    List<ClientesPorMes> clientesPorMes();

    interface ClientesPorMes {

        String getMes();

        Long getTotal();
    }
}
