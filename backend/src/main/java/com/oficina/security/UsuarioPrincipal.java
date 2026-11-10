package com.oficina.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Usuario autenticado pela sessao: guarda id, email e autoridade. */
public class UsuarioPrincipal implements UserDetails, CredentialsContainer {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String email;
    private String senhaHash;
    private final String perfil;
    private final boolean ativo;
    private final boolean bloqueado;

    public UsuarioPrincipal(Long id, String email, String senhaHash, String perfil, boolean ativo, boolean bloqueado) {
        this.id = id;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = ativo;
        this.bloqueado = bloqueado;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPerfil() { return perfil; }
    public String getPerfilRotulo() { return Perfis.rotulo(perfil); }
    public boolean isAdmin() { return Perfis.ADMIN_MASTER.equals(perfil); }
    public boolean isGerente() { return Perfis.GERENTE.equals(perfil); }
    public boolean isMecanico() { return Perfis.MECANICO.equals(perfil); }
    public boolean isAssistente() { return Perfis.ASSISTENTE_ADMINISTRATIVO.equals(perfil); }
    public boolean isFuncionario() { return Perfis.FUNCIONARIO.equals(perfil); }
    public boolean isGerenteOuAdmin() { return isAdmin() || isGerente(); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(perfil));
    }
    @Override public String getPassword() { return senhaHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !bloqueado; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return ativo; }
    @Override public void eraseCredentials() { senhaHash = null; }
}
