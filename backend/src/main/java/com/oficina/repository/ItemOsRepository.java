package com.oficina.repository;

import com.oficina.model.ItemOs;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOsRepository extends JpaRepository<ItemOs, Long> {

    List<ItemOs> findByOsIdOrderById(Long osId);

    Optional<ItemOs> findByIdAndOsId(Long id, Long osId);

    boolean existsByPecaId(Long pecaId);
}
