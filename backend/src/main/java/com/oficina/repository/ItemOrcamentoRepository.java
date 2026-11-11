package com.oficina.repository;

import com.oficina.model.ItemOrcamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {

    /**
     * Carrega os itens e suas pecas em uma unica consulta, ainda dentro da transacao.
     * Assim o Thymeleaf pode acessar item.peca.nome apos o retorno do service,
     * sem LazyInitializationException. O LEFT JOIN preserva itens de SERVICO,
     * que nao possuem uma peca associada.
     */
    @Query("SELECT item FROM ItemOrcamento item "
            + "LEFT JOIN FETCH item.peca "
            + "WHERE item.orcamento.id = :orcamentoId "
            + "ORDER BY item.id ASC")
    List<ItemOrcamento> findByOrcamentoIdOrderByIdAsc(@Param("orcamentoId") Long orcamentoId);
}
