package com.oficina.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuário autenticado, guardado na sessão. Leva o id e o perfil para os serviços decidirem o que
 * cada pessoa pode fazer, sem consultar o banco de novo. O hash da senha é apagado depois do login.
 */
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

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPerfil() {
        return perfil;
    }

    /** Nome do perfil para mostrar na tela (Gerente, Funcionário...). */
    public String getPerfilRotulo() {
        return Perfis.rotulo(perfil);
    }

    public boolean isAdmin() {
        return Perfis.ADMIN_MASTER.equals(perfil);
    }

    public boolean isGerente() {
        return Perfis.GERENTE.equals(perfil);
    }

    public boolean isFuncionario() {
        return Perfis.FUNCIONARIO.equals(perfil);
    }

    public boolean isGerenteOuAdmin() {
        return isAdmin() || isGerente();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(perfil));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    @Override
    public void eraseCredentials() {
        this.senhaHash = null;
    }
}
