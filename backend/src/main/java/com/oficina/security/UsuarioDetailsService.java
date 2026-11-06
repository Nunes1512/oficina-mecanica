package com.oficina.security;

import com.oficina.model.Usuario;
import com.oficina.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega o usuário do banco (tabela usuario) para o login. */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;
    private final LoginAttemptService tentativas;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Credenciais inválidas");
        }
        Usuario usuario = usuarios.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
        boolean bloqueado = usuario.isBloqueado() || tentativas.bloqueado(usuario.getEmail());
        return new UsuarioPrincipal(usuario.getId(), usuario.getEmail(), usuario.getSenhaHash(),
                usuario.getFuncao().getNome(), usuario.isAtivo(), bloqueado);
    }
}
