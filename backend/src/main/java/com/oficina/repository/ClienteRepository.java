package com.oficina.repository;

import com.oficina.model.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    List<Cliente> findAllByOrderByNome();

    List<Cliente> findByNomeContainingIgnoreCaseOrCpfContainingOrderByNome(String nome, String cpf);
}
