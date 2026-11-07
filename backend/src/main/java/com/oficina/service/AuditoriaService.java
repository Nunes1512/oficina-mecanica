package com.oficina.service;

import com.oficina.model.Auditoria;
import com.oficina.repository.AuditoriaRepository;
import com.oficina.repository.UsuarioRepository;
import com.oficina.security.UsuarioPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra quem fez o quê nas ações críticas. A tabela só aceita INSERT, então o histórico não se altera. */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditorias;
    private final UsuarioRepository usuarios;

    /** Não grave CPF, senha ou outros dados pessoais em "detalhes". */
    @Transactional
    public void registrar(UsuarioPrincipal ator, String acao, String entidade, Long entidadeId, String detalhes) {
        Auditoria registro = new Auditoria();
        if (ator != null) {
            registro.setUsuario(usuarios.getReferenceById(ator.getId()));
        }
        registro.setAcao(acao);
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setDetalhes(detalhes);
        auditorias.save(registro);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> ultimos() {
        return auditorias.findTop100ByOrderByDataDesc();
    }
}
