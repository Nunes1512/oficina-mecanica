package com.oficina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.oficina.form.FuncionarioForm;
import com.oficina.model.Funcionario;
import com.oficina.model.StatusFuncionario;
import com.oficina.repository.FuncionarioRepository;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarios;
    @Mock
    private AuditoriaService auditoria;
    @InjectMocks
    private FuncionarioService service;

    private FuncionarioForm form(BigDecimal salario) {
        FuncionarioForm form = new FuncionarioForm();
        form.setNome("Ana");
        form.setCpf("12345678909");
        form.setSalario(salario);
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
    void gerenteNaoDefineSalarioAoCriar() {
        when(funcionarios.existsByCpf(any())).thenReturn(false);
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario criado = service.criar(form(new BigDecimal("9999.00")), gerente());

        assertThat(criado.getSalario()).isEqualByComparingTo("0");
    }

    @Test
    void adminDefineSalarioAoCriar() {
        when(funcionarios.existsByCpf(any())).thenReturn(false);
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Funcionario criado = service.criar(form(new BigDecimal("3500.00")), admin());

        assertThat(criado.getSalario()).isEqualByComparingTo("3500.00");
    }

    @Test
    void gerenteNaoAlteraSalarioAoEditarMesmoEnviandoOCampo() {
        Funcionario existente = new Funcionario();
        existente.setSalario(new BigDecimal("3000.00"));
        when(funcionarios.findById(5L)).thenReturn(Optional.of(existente));
        when(funcionarios.existsByCpfAndIdNot(any(), eq(5L))).thenReturn(false);

        service.atualizar(5L, form(new BigDecimal("1.00")), gerente());

        assertThat(existente.getSalario()).isEqualByComparingTo("3000.00");
    }

    @Test
    void adminAlteraSalarioAoEditar() {
        Funcionario existente = new Funcionario();
        existente.setSalario(new BigDecimal("3000.00"));
        when(funcionarios.findById(5L)).thenReturn(Optional.of(existente));
        when(funcionarios.existsByCpfAndIdNot(any(), eq(5L))).thenReturn(false);

        service.atualizar(5L, form(new BigDecimal("4200.00")), admin());

        assertThat(existente.getSalario()).isEqualByComparingTo("4200.00");
    }
}
