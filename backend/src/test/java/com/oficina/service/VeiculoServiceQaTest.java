package com.oficina.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.oficina.exception.NegocioException;
import com.oficina.form.VeiculoForm;
import com.oficina.model.Cliente;
import com.oficina.model.Veiculo;
import com.oficina.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class VeiculoServiceQaTest {
    @Mock VeiculoRepository veiculos;
    @Mock ClienteRepository clientes;
    @Mock TimelineVeiculoRepository timeline;
    @Mock OrdemServicoRepository ordens;
    @Mock AuditoriaService auditoria;
    @InjectMocks VeiculoService service;

    @Test void preservaProprietarioDeVeiculoComHistorico() {
        Cliente antigo = new Cliente(); antigo.setId(1L);
        Veiculo veiculo = new Veiculo();
        veiculo.setId(5L);
        veiculo.setCliente(antigo);
        veiculo.setPlaca("ABC1D23");
        when(veiculos.findById(5L)).thenReturn(Optional.of(veiculo));
        when(ordens.countByVeiculoId(5L)).thenReturn(1L);
        VeiculoForm form = new VeiculoForm();
        form.setClienteId(2L);
        form.setPlaca("ABC1D23");
        assertThrows(NegocioException.class, () -> service.atualizar(5L, form));
        assertEquals(1L, veiculo.getCliente().getId());
        verifyNoInteractions(clientes);
    }
}
