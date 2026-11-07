package com.oficina.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficina.exception.NegocioException;
import com.oficina.form.ClienteForm;
import com.oficina.model.Cliente;
import com.oficina.repository.ClienteRepository;
import com.oficina.repository.VeiculoRepository;
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
class ClienteServiceTest {

    @Mock
    private ClienteRepository clientes;
    @Mock
    private VeiculoRepository veiculos;
    @Mock
    private AuditoriaService auditoria;
    @InjectMocks
    private ClienteService service;

    private ClienteForm form(String cpf) {
        ClienteForm form = new ClienteForm();
        form.setNome("Ana");
        form.setCpf(cpf);
        return form;
    }

    @Test
    void recusaCpfDuplicado() {
        when(clientes.existsByCpf("123.456.789-09")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(form("12345678909")))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("CPF");
        verify(clientes, never()).save(any());
    }

    @Test
    void guardaOCpfNoFormatoDoBanco() {
        when(clientes.existsByCpf(anyString())).thenReturn(false);
        when(clientes.save(any(Cliente.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Cliente salvo = service.criar(form("12345678909"));

        assertThat(salvo.getCpf()).isEqualTo("123.456.789-09");
        assertThat(salvo.getTelefone()).isNull();
    }

    @Test
    void naoExcluiClienteQueTemVeiculos() {
        Cliente cliente = new Cliente();
        cliente.setNome("Ana");
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente));
        when(veiculos.countByClienteId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.excluir(1L, null)).isInstanceOf(NegocioException.class);
        verify(clientes, never()).delete(any(Cliente.class));
    }

    @Test
    void excluiClienteSemVeiculosERegistraAuditoria() {
        Cliente cliente = new Cliente();
        cliente.setNome("Ana");
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente));
        when(veiculos.countByClienteId(1L)).thenReturn(0L);

        service.excluir(1L, null);

        verify(clientes).delete(cliente);
        verify(auditoria).registrar(isNull(), eq("EXCLUIR"), eq("cliente"), eq(1L), anyString());
    }
}
