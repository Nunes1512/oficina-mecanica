package com.oficina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock private FuncionarioRepository funcionarios;
    @Mock private UsuarioRepository usuarios;
    @Mock private FuncaoRepository funcoes;
    @Mock private PasswordEncoder encoder;
    @Mock private AuditoriaService auditoria;
    @InjectMocks private FuncionarioService service;

    private FuncionarioForm form() {
        FuncionarioForm form = new FuncionarioForm();
        form.setNome("Ana");
        form.setCpf("12345678909");
        form.setStatus(StatusFuncionario.ATIVO);
        return form;
    }

    private UsuarioPrincipal gerente() {
        return new UsuarioPrincipal(1L, "gerente@oficina.com", null, Perfis.GERENTE, true, false);
    }

    private UsuarioPrincipal admin() {
        return new UsuarioPrincipal(2L, "admin@oficina.com", null, Perfis.ADMIN_MASTER, true, false);
    }

    @Test
    void gerentePodeCadastrarFuncionarioSemUsuario() {
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario criado = service.criar(form(), gerente());

        assertThat(criado.getUsuario()).isNull();
        assertThat(criado.getSalario()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void gerenteNaoPodeCriarUsuarioMesmoForcandoOsCampos() {
        FuncionarioForm form = form();
        form.setCriarUsuario(true);
        form.setEmailUsuario("novo@oficina.com");
        form.setSenhaInicial("SenhaForte#123");
        form.setPerfilUsuario(Perfis.FUNCIONARIO);

        assertThatThrownBy(() -> service.criar(form, gerente()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Somente o Admin Master");
        verify(funcionarios, never()).save(any(Funcionario.class));
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void adminPodeCadastrarFuncionarioComUsuario() {
        FuncionarioForm form = form();
        form.setCriarUsuario(true);
        form.setEmailUsuario("Ana@Oficina.com");
        form.setSenhaInicial("SenhaForte#123");
        form.setPerfilUsuario(Perfis.FUNCIONARIO);

        Funcao funcao = new Funcao();
        funcao.setNome(Perfis.FUNCIONARIO);
        when(funcoes.findByNome(Perfis.FUNCIONARIO)).thenReturn(Optional.of(funcao));
        when(encoder.encode("SenhaForte#123")).thenReturn("hash-bcrypt");
        when(usuarios.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario criado = service.criar(form, admin());

        assertThat(criado.getUsuario()).isNotNull();
        assertThat(criado.getUsuario().getEmail()).isEqualTo("ana@oficina.com");
        assertThat(criado.getUsuario().getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(criado.getUsuario().getFuncao().getNome()).isEqualTo(Perfis.FUNCIONARIO);
        verify(usuarios).save(any(Usuario.class));
    }

    @Test
    void adminNaoPodeCriarOutroAdminMasterPeloCadastro() {
        FuncionarioForm form = form();
        form.setCriarUsuario(true);
        form.setEmailUsuario("novo@oficina.com");
        form.setSenhaInicial("SenhaForte#123");
        form.setPerfilUsuario(Perfis.ADMIN_MASTER);

        assertThatThrownBy(() -> service.criar(form, admin()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("perfil de acesso válido");
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void impedeEmailJaRegistrado() {
        FuncionarioForm form = form();
        form.setCriarUsuario(true);
        form.setEmailUsuario("usado@oficina.com");
        form.setSenhaInicial("SenhaForte#123");
        form.setPerfilUsuario(Perfis.FUNCIONARIO);
        when(usuarios.existsByEmailIgnoreCase("usado@oficina.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(form, admin()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Já existe um usuário");
        verify(funcionarios, never()).save(any(Funcionario.class));
    }

    @Test
    void adminPodeCriarLoginParaFuncionarioExistenteSemConta() {
        FuncionarioForm form = form();
        form.setCriarUsuario(true);
        form.setEmailUsuario("caua@oficina.com");
        form.setSenhaInicial("SenhaForte#123");
        form.setPerfilUsuario(Perfis.FUNCIONARIO);

        Funcionario existente = new Funcionario();
        existente.setId(9L);
        existente.setSalario(new BigDecimal("2500.00"));
        when(funcionarios.findById(9L)).thenReturn(Optional.of(existente));
        Funcao funcao = new Funcao();
        funcao.setNome(Perfis.FUNCIONARIO);
        when(funcoes.findByNome(Perfis.FUNCIONARIO)).thenReturn(Optional.of(funcao));
        when(encoder.encode("SenhaForte#123")).thenReturn("hash-bcrypt");
        when(usuarios.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario atualizado = service.atualizar(9L, form, admin());

        assertThat(atualizado.getUsuario()).isNotNull();
        assertThat(atualizado.getUsuario().getEmail()).isEqualTo("caua@oficina.com");
        assertThat(atualizado.getSalario()).isEqualByComparingTo("2500.00");
        verify(usuarios).save(any(Usuario.class));
        verify(funcionarios).save(existente);
    }

    @Test
    void gerenteNaoPodeCriarLoginEditandoFuncionario() {
        FuncionarioForm form = form();
        form.setCriarUsuario(true);
        assertThatThrownBy(() -> service.atualizar(9L, form, gerente()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Somente o Admin Master");
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void naoPermiteSegundoLoginParaMesmoFuncionario() {
        FuncionarioForm form = form();
        form.setCriarUsuario(true);
        Funcionario existente = new Funcionario();
        existente.setUsuario(new Usuario());
        when(funcionarios.findById(9L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.atualizar(9L, form, admin()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("já possui um usuário");
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void edicaoNaoAlteraSalarioAnterior() {
        Funcionario existente = new Funcionario();
        existente.setSalario(new BigDecimal("3000.00"));
        when(funcionarios.findById(5L)).thenReturn(Optional.of(existente));
        when(funcionarios.existsByCpfAndIdNot(any(), eq(5L))).thenReturn(false);

        service.atualizar(5L, form(), admin());

        assertThat(existente.getSalario()).isEqualByComparingTo("3000.00");
        verify(usuarios, never()).save(any(Usuario.class));
    }
    @Test
    void adminCriaContaAoPreencherLoginMesmoSemCheckbox() {
        FuncionarioForm formulario = form();
        formulario.setEmailUsuario("caua@oficina.com");
        formulario.setSenhaInicial("SenhaForte#123");
        formulario.setPerfilUsuario(Perfis.FUNCIONARIO);
        // Checkbox NÃO marcado: situação do bug original.
        assertThat(formulario.isCriarUsuario()).isFalse();
        Funcao funcao = new Funcao();
        funcao.setNome(Perfis.FUNCIONARIO);
        when(funcoes.findByNome(Perfis.FUNCIONARIO)).thenReturn(Optional.of(funcao));
        when(encoder.encode("SenhaForte#123")).thenReturn("hash-bcrypt");
        when(usuarios.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario funcionario = service.criar(formulario, admin());

        assertThat(funcionario.getUsuario()).isNotNull();
        assertThat(funcionario.getUsuario().getEmail()).isEqualTo("caua@oficina.com");
        verify(usuarios).save(any(Usuario.class));
    }

    @Test
    void naoSalvaFuncionarioSeDadosDeLoginFaltam() {
        FuncionarioForm formulario = form();
        formulario.setEmailUsuario("caua@oficina.com");
        formulario.setPerfilUsuario(Perfis.FUNCIONARIO);
        // Senha ausente: deve informar erro, não salvar cadastro sem conta.
        assertThatThrownBy(() -> service.criar(formulario, admin()))
                .isInstanceOf(NegocioException.class);
        verify(funcionarios, never()).save(any(Funcionario.class));
    }

    @Test
    void gerenteNaoPodeCriarContaNemComCheckboxDesmarcado() {
        FuncionarioForm formulario = form();
        formulario.setEmailUsuario("caua@oficina.com");
        formulario.setSenhaInicial("SenhaForte#123");
        formulario.setPerfilUsuario(Perfis.FUNCIONARIO);
        assertThatThrownBy(() -> service.criar(formulario, gerente()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Somente o Admin Master");
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void adminPodeVincularContaAoEditarSemCheckbox() {
        FuncionarioForm formulario = form();
        formulario.setEmailUsuario("caua@oficina.com");
        formulario.setSenhaInicial("SenhaForte#123");
        formulario.setPerfilUsuario(Perfis.FUNCIONARIO);
        Funcionario existente = new Funcionario();
        existente.setId(7L);
        when(funcionarios.findById(7L)).thenReturn(Optional.of(existente));
        Funcao funcao = new Funcao();
        funcao.setNome(Perfis.FUNCIONARIO);
        when(funcoes.findByNome(Perfis.FUNCIONARIO)).thenReturn(Optional.of(funcao));
        when(encoder.encode("SenhaForte#123")).thenReturn("hash-bcrypt");
        when(usuarios.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario atualizado = service.atualizar(7L, formulario, admin());

        assertThat(atualizado.getUsuario()).isNotNull();
        assertThat(atualizado.getUsuario().getEmail()).isEqualTo("caua@oficina.com");
        verify(usuarios).save(any(Usuario.class));
    }

}
