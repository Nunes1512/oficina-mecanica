package com.oficina.repository;

import com.oficina.model.Peca;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PecaRepository extends JpaRepository<Peca, Long> {

    List<Peca> findAllByOrderByNome();

    List<Peca> findByNomeContainingIgnoreCaseOrderByNome(String nome);

    /** Peças no estoque mínimo ou abaixo dele. */
    @Query("select p from Peca p where p.quantidade <= p.estoqueMinimo order by p.quantidade, p.nome")
    List<Peca> findEstoqueBaixo();

    /** Trava a linha da peça até o fim da transação, para duas entradas simultâneas não se atropelarem. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Peca p where p.id = :id")
    Optional<Peca> buscarParaAtualizar(@Param("id") Long id);

    /** Soma ao saldo direto no banco (a coluna é somente leitura na entidade, de propósito). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE peca SET quantidade = quantidade + :qtd WHERE id = :id", nativeQuery = true)
    int somarQuantidade(@Param("id") Long id, @Param("qtd") int quantidade);
}
