package com.oficina.repository;

import com.oficina.model.Notificacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuarioIdOrderByDataDesc(Long usuarioId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);

    /** Busca já filtrando pelo dono: impede ler ou marcar a notificação de outro usuário. */
    Optional<Notificacao> findByIdAndUsuarioId(Long id, Long usuarioId);
}
