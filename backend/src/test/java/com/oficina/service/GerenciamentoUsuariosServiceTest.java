package com.oficina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficina.exception.NegocioException;
import com.oficina.model.Funcao;
import com.oficina.model.Usuario;
import com.oficina.repository.FuncaoRepository;
import com.oficina.repository.UsuarioRepository;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class GerenciamentoUsuariosServiceTest {

    @Mock UsuarioRepository usuarios;
    @Mock FuncaoRepository funcoes;
    @Mock PasswordEncoder encoder;
    @Mock AuditoriaService auditoria;
    @Mock SessionRegistry sessoes;
    @InjectMocks GerenciamentoUsuariosService service;

    private UsuarioPrincipal admin() {
        return new UsuarioPrincipal(1L, "admin@oficina.com", null, Perfis.ADMIN_MASTER, true, false);
    }

    private Usuario legado(Long id) {
        Funcao funcao = new Funcao();
        funcao.setNome(Perfis.FUNCIONARIO);
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setFuncao(funcao);
        return usuario;
    }

    @Test
    void adminPodeAlterarPerfilAntigoParaMecanico() {
        Usuario conta = legado(2L);
        Funcao mecanico = new Funcao();
        mecanico.setNome(Perfis.MECANICO);
        when(usuarios.findById(2L)).thenReturn(Optional.of(conta));
        when(funcoes.findByNome(Perfis.MECANICO)).thenReturn(Optional.of(mecanico));
        when(sessoes.getAllPrincipals()).thenReturn(List.of());

        service.alterarPerfil(2L, Perfis.MECANICO, admin());

        assertThat(conta.getFuncao().getNome()).isEqualTo(Perfis.MECANICO);
        verify(usuarios).saveAndFlush(conta);
    }

    @Test
    void naoPermiteModificarAdminMasterDeOutrasContas() {
        Usuario conta = legado(2L);
        Funcao funcao = new Funcao();
        funcao.setNome(Perfis.ADMIN_MASTER);
        conta.setFuncao(funcao);
        when(usuarios.findById(2L)).thenReturn(Optional.of(conta));
        assertThatThrownBy(() -> service.alterarPerfil(2L, Perfis.GERENTE, admin()))
                .isInstanceOf(NegocioException.class);
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void perfilAntigoNaoPodeSerAtribuidoNovamente() {
        Usuario conta = legado(2L);
        when(usuarios.findById(2L)).thenReturn(Optional.of(conta));
        assertThatThrownBy(() -> service.alterarPerfil(2L, Perfis.FUNCIONARIO, admin()))
                .isInstanceOf(NegocioException.class);
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void senhaAtualIncorretaBloqueiaTroca() {
        Usuario conta = legado(2L);
        conta.setSenhaHash("hash-atual");
        when(usuarios.findById(2L)).thenReturn(Optional.of(conta));
        when(encoder.matches("senha-incorreta", "hash-atual")).thenReturn(false);
        UsuarioPrincipal ator = new UsuarioPrincipal(2L, "mecanico@oficina.com", null,
                Perfis.MECANICO, true, false);
        assertThatThrownBy(() -> service.trocarMinhaSenha(
                "senha-incorreta", "NovaSenha#123", "NovaSenha#123", ator))
                .isInstanceOf(NegocioException.class);
        verify(usuarios, never()).saveAndFlush(any());
    }
}
