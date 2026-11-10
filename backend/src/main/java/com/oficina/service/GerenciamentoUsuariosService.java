package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.model.Funcao;
import com.oficina.model.Usuario;
import com.oficina.repository.FuncaoRepository;
import com.oficina.repository.UsuarioRepository;
import com.oficina.security.PasswordPolicy;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administração de acessos: sem exibir ou recuperar senhas existentes. */
@Service
@RequiredArgsConstructor
@Transactional
public class GerenciamentoUsuariosService {

    private final UsuarioRepository usuarios;
    private final FuncaoRepository funcoes;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;
    private final SessionRegistry sessoes;

    @PreAuthorize("hasRole('ADMIN_MASTER')")
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAllByOrderByEmail();
    }

    @PreAuthorize("hasRole('ADMIN_MASTER')")
    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public void alterarPerfil(Long id, String perfil, UsuarioPrincipal ator) {
        Usuario usuario = usuarioEditavel(id, ator);
        if (!perfilPermitido(perfil)) {
            throw new NegocioException("Selecione Mecânico, Assistente Administrativo ou Gerente.");
        }
        Funcao funcao = funcoes.findByNome(perfil)
                .orElseThrow(() -> new NegocioException("O perfil selecionado não existe no banco."));
        if (Objects.equals(usuario.getFuncao().getNome(), perfil)) {
            return;
        }
        String anterior = usuario.getFuncao().getNome();
        usuario.setFuncao(funcao);
        usuarios.saveAndFlush(usuario);
        auditoria.registrar(ator, "PERFIL", "usuario", id,
                "Alteração de perfil: " + anterior + " -> " + perfil);
        encerrarSessoes(id);
    }

    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public void redefinirSenha(Long id, String novaSenha, String confirmacao, UsuarioPrincipal ator) {
        Usuario usuario = usuarioEditavel(id, ator);
        conferirNovaSenha(novaSenha, confirmacao);
        usuario.setSenhaHash(encoder.encode(novaSenha));
        usuarios.saveAndFlush(usuario);
        auditoria.registrar(ator, "SENHA", "usuario", id, "Senha redefinida pelo Admin Master");
        encerrarSessoes(id);
    }

    @PreAuthorize("isAuthenticated()")
    public void trocarMinhaSenha(String atual, String novaSenha, String confirmacao,
                                 UsuarioPrincipal ator) {
        if (ator == null) {
            throw new NegocioException("É necessário estar autenticado.");
        }
        Usuario usuario = usuarios.findById(ator.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (!encoder.matches(atual == null ? "" : atual, usuario.getSenhaHash())) {
            throw new NegocioException("A senha atual está incorreta.");
        }
        conferirNovaSenha(novaSenha, confirmacao);
        if (encoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new NegocioException("A nova senha deve ser diferente da atual.");
        }
        usuario.setSenhaHash(encoder.encode(novaSenha));
        usuarios.saveAndFlush(usuario);
        auditoria.registrar(ator, "SENHA", "usuario", usuario.getId(), "Troca de senha da própria conta");
        encerrarSessoes(usuario.getId());
    }

    private Usuario usuarioEditavel(Long id, UsuarioPrincipal ator) {
        if (ator == null || !ator.isAdmin()) {
            throw new NegocioException("Somente o Admin Master pode administrar usuários.");
        }
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (Objects.equals(ator.getId(), id) || Perfis.ADMIN_MASTER.equals(usuario.getFuncao().getNome())) {
            throw new NegocioException("O perfil e a senha do Admin Master só podem ser alterados na própria conta.");
        }
        return usuario;
    }

    private boolean perfilPermitido(String perfil) {
        return Perfis.MECANICO.equals(perfil)
                || Perfis.ASSISTENTE_ADMINISTRATIVO.equals(perfil)
                || Perfis.GERENTE.equals(perfil);
    }

    private void conferirNovaSenha(String novaSenha, String confirmacao) {
        if (!PasswordPolicy.forte(novaSenha)) {
            throw new NegocioException(PasswordPolicy.MENSAGEM);
        }
        if (!Objects.equals(novaSenha, confirmacao)) {
            throw new NegocioException("A confirmação da senha não confere.");
        }
    }

    /** Expira todas as sessões ativas do usuário alterado (sem depender de logoff manual). */
    private void encerrarSessoes(Long id) {
        for (Object principal : sessoes.getAllPrincipals()) {
            if (principal instanceof UsuarioPrincipal p && Objects.equals(p.getId(), id)) {
                for (SessionInformation sessao : sessoes.getAllSessions(principal, false)) {
                    sessao.expireNow();
                }
            }
        }
    }
}
