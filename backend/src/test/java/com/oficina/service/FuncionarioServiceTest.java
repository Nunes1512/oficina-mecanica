package com.oficina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficina.exception.NegocioException;
import com.oficina.form.FuncionarioForm;
import com.oficina.model.Funcao;
import com.oficina.model.Funcionario;
import com.oficina.model.StatusFuncionario;
import com.oficina.model.Usuario;
import com.oficina.repository.FuncaoRepository;
import com.oficina.repository.FuncionarioRepository;
import com.oficina.repository.UsuarioRepository;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {
    @Mock private FuncionarioRepository funcionarios;
    @Mock private UsuarioRepository usuarios;
    @Mock private FuncaoRepository funcoes;
    @Mock private PasswordEncoder encoder;
    @Mock private AuditoriaService auditoria;
    @Mock private SessionRegistry sessoes;
    @InjectMocks private FuncionarioService service;

    private FuncionarioForm form() {
        FuncionarioForm f = new FuncionarioForm();
        f.setNome("Ana");
        f.setCpf("12345678909");
        f.setEmail("ana@oficina.com");
        f.setStatus(StatusFuncionario.ATIVO);
        f.setSenhaInicial("SenhaForte#123");
        f.setPerfilUsuario(Perfis.MECANICO);
        return f;
    }

    private UsuarioPrincipal admin() {
        return new UsuarioPrincipal(10L, "admin@oficina.com", null, Perfis.ADMIN_MASTER, true, false);
    }

    private UsuarioPrincipal gerente() {
        return new UsuarioPrincipal(20L, "gerente@oficina.com", null, Perfis.GERENTE, true, false);
    }

    @Test
    void cadastroCriaFuncionarioEUsuarioAutomaticamente() {
        Funcao papel = new Funcao();
        papel.setNome(Perfis.MECANICO);
        when(funcoes.findByNome(Perfis.MECANICO)).thenReturn(Optional.of(papel));
        when(encoder.encode("SenhaForte#123")).thenReturn("hash-bcrypt");
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        Funcionario funcionario = service.criar(form(), admin());
        assertThat(funcionario.getUsuario()).isNotNull();
        assertThat(funcionario.getUsuario().getEmail()).isEqualTo("ana@oficina.com");
        assertThat(funcionario.getUsuario().getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(funcionario.getUsuario().getFuncao().getNome()).isEqualTo(Perfis.MECANICO);
        assertThat(funcionario.getSalario()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(usuarios).save(any(Usuario.class));
        verify(funcionarios).save(any(Funcionario.class));
    }

    @Test
    void gerenteNaoPodeCriarFuncionarioQueCriariaConta() {
        assertThatThrownBy(() -> service.criar(form(), gerente()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Somente o Admin Master");
        verify(funcionarios, never()).save(any(Funcionario.class));
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void naoCadastraFuncionarioSemSenhaOuPerfil() {
        FuncionarioForm f = form();
        f.setSenhaInicial(null);
        assertThatThrownBy(() -> service.criar(f, admin()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("senha");
        verify(funcionarios, never()).save(any(Funcionario.class));
    }

    @Test
    void impedePerfilAntigoEmNovosCadastros() {
        FuncionarioForm f = form();
        f.setPerfilUsuario(Perfis.FUNCIONARIO);
        assertThatThrownBy(() -> service.criar(f, admin()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("perfil válido");
        verify(funcionarios, never()).save(any(Funcionario.class));
    }

    @Test
    void naoCriaContaDuplicada() {
        when(usuarios.existsByEmailIgnoreCase("ana@oficina.com")).thenReturn(true);
        assertThatThrownBy(() -> service.criar(form(), admin()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Já existe um usuário");
        verify(funcionarios, never()).save(any(Funcionario.class));
    }

    @Test
    void edicaoDeFuncionarioComContaNaoCriaOutra() {
        Funcionario existente = new Funcionario();
        existente.setId(42L);
        existente.setUsuario(new Usuario());
        existente.setSalario(new BigDecimal("2500.00"));
        when(funcionarios.findById(42L)).thenReturn(Optional.of(existente));
        FuncionarioForm f = form();
        f.setSenhaInicial(null);
        f.setPerfilUsuario(null);
        Funcionario atualizado = service.atualizar(42L, f, gerente());
        assertThat(atualizado.getSalario()).isEqualByComparingTo("2500.00");
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void adminAtivaAcessoParaFuncionarioLegadoAoEditar() {
        Funcionario legado = new Funcionario();
        legado.setId(33L);
        legado.setSalario(BigDecimal.ZERO);
        when(funcionarios.findById(33L)).thenReturn(Optional.of(legado));
        Funcao papel = new Funcao();
        papel.setNome(Perfis.MECANICO);
        when(funcoes.findByNome(Perfis.MECANICO)).thenReturn(Optional.of(papel));
        when(encoder.encode("SenhaForte#123")).thenReturn("hash");
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        Funcionario atualizado = service.atualizar(33L, form(), admin());
        assertThat(atualizado.getUsuario()).isNotNull();
        verify(usuarios).save(any(Usuario.class));
    }

    @Test
    void gerenteNaoPodeVincularAcessoAFuncionarioLegado() {
        Funcionario legado = new Funcionario();
        when(funcionarios.findById(33L)).thenReturn(Optional.of(legado));
        assertThatThrownBy(() -> service.atualizar(33L, form(), gerente()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Somente o Admin Master");
        verify(usuarios, never()).save(any(Usuario.class));
    }
}
