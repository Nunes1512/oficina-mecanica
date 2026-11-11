package com.oficina.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.oficina.exception.NegocioException;
import com.oficina.form.EntradaEstoqueForm;
import com.oficina.form.PecaForm;
import com.oficina.model.ItemOrcamento;
import com.oficina.model.MovimentacaoEstoque;
import com.oficina.model.Peca;
import com.oficina.repository.MovimentacaoEstoqueRepository;
import com.oficina.repository.PecaRepository;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CatalogoPecasServiceTest {
    @Mock PecaRepository pecas;
    @Mock MovimentacaoEstoqueRepository movimentacoes;
    @Mock AuditoriaService auditoria;
    @InjectMocks CatalogoPecasService service;

    private UsuarioPrincipal gerente() {
        return new UsuarioPrincipal(1L, "gestao@oficina.com", "hash", Perfis.GERENTE, true, false);
    }
    private UsuarioPrincipal mecanico() {
        return new UsuarioPrincipal(9L, "mecanico@oficina.com", "hash", Perfis.MECANICO, true, false);
    }
    private PecaForm formulario(BigDecimal preco) {
        PecaForm f = new PecaForm();
        f.setNome("Bateria 60Ah");
        f.setFabricante("Moura");
        f.setValorUnitario(preco);
        f.setEstoqueMinimo(3);
        return f;
    }

    @Test
    void cadastraPrecoDeVendaSemPermitirInformarSaldoNoCadastro() {
        when(pecas.saveAndFlush(any(Peca.class))).thenAnswer(inv -> {
            Peca p = inv.getArgument(0);
            p.setId(8L);
            return p;
        });
        Peca peca = service.cadastrar(formulario(new BigDecimal("489.00")), gerente());
        assertEquals(new BigDecimal("489.00"), peca.getValorUnitario());
        assertEquals(3, peca.getEstoqueMinimo());
        verify(pecas).saveAndFlush(any(Peca.class));
        verifyNoInteractions(movimentacoes);
    }

    @Test
    void mecanicoNaoPodeCadastrarOuEditarPrecos() {
        assertThrows(AccessDeniedException.class, () -> service.cadastrar(formulario(BigDecimal.ONE), mecanico()));
        assertThrows(AccessDeniedException.class, () -> service.atualizar(5L, formulario(BigDecimal.TEN), mecanico()));
        verifyNoInteractions(pecas, movimentacoes);
    }

    @Test
    void mudarPrecoDoCatalogoNaoModificaOrcamentoJaPrecificado() {
        Peca peca = new Peca();
        peca.setId(8L);
        peca.setValorUnitario(new BigDecimal("489.00"));
        peca.setNome("Bateria 60Ah");
        peca.setEstoqueMinimo(3);
        ItemOrcamento antigo = new ItemOrcamento();
        antigo.setPeca(peca);
        antigo.setValorUnitario(new BigDecimal("489.00"));
        when(pecas.findById(8L)).thenReturn(Optional.of(peca));
        service.atualizar(8L, formulario(new BigDecimal("520.00")), gerente());
        assertEquals(new BigDecimal("520.00"), peca.getValorUnitario());
        assertEquals(new BigDecimal("489.00"), antigo.getValorUnitario());
    }

    @Test
    void entradaRegistraCustoDeCompraEDiferenciaDoPrecoDeVenda() {
        Peca peca = new Peca();
        peca.setId(8L);
        peca.setQuantidade(2);
        peca.setValorUnitario(new BigDecimal("489.00"));
        when(pecas.buscarParaAtualizar(8L)).thenReturn(Optional.of(peca));
        when(pecas.somarQuantidade(8L, 5)).thenReturn(1);
        EntradaEstoqueForm entrada = new EntradaEstoqueForm();
        entrada.setQuantidade(5);
        entrada.setCustoUnitario(new BigDecimal("350.00"));
        service.registrarEntrada(8L, entrada, gerente());
        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacoes).save(captor.capture());
        assertEquals(5, captor.getValue().getQuantidade());
        assertEquals(new BigDecimal("350.00"), captor.getValue().getCustoUnitario());
        assertEquals(new BigDecimal("489.00"), peca.getValorUnitario());
        verify(pecas).somarQuantidade(eq(8L), eq(5));
    }

    @Test
    void impedeEntradaComCustoNegativo() {
        EntradaEstoqueForm entrada = new EntradaEstoqueForm();
        entrada.setQuantidade(1);
        entrada.setCustoUnitario(new BigDecimal("-0.01"));
        assertThrows(NegocioException.class, () -> service.registrarEntrada(8L, entrada, gerente()));
        verifyNoInteractions(pecas, movimentacoes);
    }
}
