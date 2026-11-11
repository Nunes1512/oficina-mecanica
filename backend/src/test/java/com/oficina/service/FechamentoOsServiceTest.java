package com.oficina.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.oficina.exception.NegocioException;
import com.oficina.model.*;
import com.oficina.repository.*;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FechamentoOsServiceTest {
    @Mock OrdemServicoRepository ordens;
    @Mock PagamentoOsRepository pagamentos;
    @Mock RetiradaOsRepository retiradas;
    @Mock UsuarioRepository usuarios;
    @Mock AuditoriaService auditoria;
    @InjectMocks FechamentoOsService fechamento;

    private UsuarioPrincipal ator(String perfil) {
        return new UsuarioPrincipal(17L, "teste@oficina.com", "hash", perfil, true, false);
    }

    private OrdemServico os(StatusOs status) {
        OrdemServico os = new OrdemServico();
        os.setId(49L);
        os.setNumero("OS-TESTE");
        os.setStatus(status);
        os.setValorTotal(new BigDecimal("385.50"));
        Veiculo veiculo = new Veiculo();
        veiculo.setId(7L);
        veiculo.setStatus(StatusVeiculo.CONCLUIDO);
        os.setVeiculo(veiculo);
        return os;
    }

    private void travar(OrdemServico os) {
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
    }

    @Test
    void gerenteLiberaPagamentoSomenteAposConclusaoTecnica() {
        OrdemServico os = os(StatusOs.CONCLUIDA);
        travar(os);
        fechamento.liberarPagamento(49L, ator(Perfis.GERENTE));
        assertEquals(StatusOs.AGUARDANDO_PAGAMENTO, os.getStatus());
        verify(auditoria).registrar(any(), eq("LIBERAR_PAGAMENTO"), eq("ordem_servico"), eq(49L), any());
    }

    @Test
    void assistenteNaoPodeLiberarPagamento() {
        assertThrows(AccessDeniedException.class,
                () -> fechamento.liberarPagamento(49L, ator(Perfis.ASSISTENTE_ADMINISTRATIVO)));
        verifyNoInteractions(ordens);
    }

    @Test
    void naoLiberaPagamentoAntesDaConclusao() {
        OrdemServico os = os(StatusOs.EM_ANDAMENTO);
        travar(os);
        assertThrows(NegocioException.class,
                () -> fechamento.liberarPagamento(49L, ator(Perfis.GERENTE)));
    }

    @Test
    void pagamentoEmDinheiroUsaValorDoBancoESegueParaRetirada() {
        OrdemServico os = os(StatusOs.AGUARDANDO_PAGAMENTO);
        travar(os);
        when(usuarios.getReferenceById(17L)).thenReturn(new Usuario());
        when(pagamentos.saveAndFlush(any(PagamentoOs.class))).thenAnswer(i -> i.getArgument(0));

        PagamentoOs p = fechamento.registrarPagamento(49L, FormaPagamentoOs.DINHEIRO,
                "Pago na recepção", null, ator(Perfis.ASSISTENTE_ADMINISTRATIVO));

        assertEquals(new BigDecimal("385.50"), p.getValor());
        assertEquals(StatusOs.AGUARDANDO_RETIRADA, os.getStatus());
        assertEquals(FormaPagamentoOs.DINHEIRO, p.getFormaPagamento());
        assertNotNull(p.getPagoEm());
        verify(pagamentos, times(1)).saveAndFlush(any(PagamentoOs.class));
    }

    @Test
    void impedePagamentoDuplicado() {
        OrdemServico os = os(StatusOs.AGUARDANDO_PAGAMENTO);
        travar(os);
        when(pagamentos.existsByOsId(49L)).thenReturn(true);
        assertThrows(NegocioException.class,
                () -> fechamento.registrarPagamento(49L, FormaPagamentoOs.DINHEIRO,
                        null, null, ator(Perfis.ASSISTENTE_ADMINISTRATIVO)));
        verify(pagamentos, never()).saveAndFlush(any());
    }

    @Test
    void exigeComprovanteParaPix() {
        OrdemServico os = os(StatusOs.AGUARDANDO_PAGAMENTO);
        travar(os);
        assertThrows(NegocioException.class,
                () -> fechamento.registrarPagamento(49L, FormaPagamentoOs.PIX,
                        null, null, ator(Perfis.ASSISTENTE_ADMINISTRATIVO)));
        verify(pagamentos, never()).saveAndFlush(any());
    }

    @Test
    void mecanicoNaoPodeRegistrarPagamento() {
        assertThrows(AccessDeniedException.class,
                () -> fechamento.registrarPagamento(49L, FormaPagamentoOs.DINHEIRO,
                        null, null, ator(Perfis.MECANICO)));
        verifyNoInteractions(ordens, pagamentos);
    }

    @Test
    void retiraVeiculoAposPagamentoEAtualizaTimelinePeloStatus() {
        OrdemServico os = os(StatusOs.AGUARDANDO_RETIRADA);
        travar(os);
        when(pagamentos.existsByOsId(49L)).thenReturn(true);
        when(usuarios.getReferenceById(17L)).thenReturn(new Usuario());
        when(retiradas.saveAndFlush(any(RetiradaOs.class))).thenAnswer(i -> i.getArgument(0));

        RetiradaOs r = fechamento.registrarRetirada(49L, "Nome do cliente", null,
                ator(Perfis.ASSISTENTE_ADMINISTRATIVO));

        assertEquals("Nome do cliente", r.getNomeRetirante());
        assertEquals(StatusOs.VEICULO_RETIRADO, os.getStatus());
        assertEquals(StatusVeiculo.ENTREGUE, os.getVeiculo().getStatus());
        verify(retiradas).saveAndFlush(any(RetiradaOs.class));
    }

    @Test
    void bloqueiaRetiradaSemPagamento() {
        OrdemServico os = os(StatusOs.AGUARDANDO_RETIRADA);
        travar(os);
        assertThrows(NegocioException.class,
                () -> fechamento.registrarRetirada(49L, "Nome", null,
                        ator(Perfis.ASSISTENTE_ADMINISTRATIVO)));
        verifyNoInteractions(usuarios);
    }

    @Test
    void gerenteEncerraSomenteDepoisDaRetirada() {
        OrdemServico os = os(StatusOs.VEICULO_RETIRADO);
        travar(os);
        when(pagamentos.existsByOsId(49L)).thenReturn(true);
        when(retiradas.existsByOsId(49L)).thenReturn(true);
        fechamento.encerrar(49L, ator(Perfis.GERENTE));
        assertEquals(StatusOs.ENCERRADA, os.getStatus());
    }

    @Test
    void assistenteNaoPodeEncerrar() {
        assertThrows(AccessDeniedException.class,
                () -> fechamento.encerrar(49L, ator(Perfis.ASSISTENTE_ADMINISTRATIVO)));
        verifyNoInteractions(ordens);
    }

    @Test
    void impedeEncerramentoAntesDaRetirada() {
        OrdemServico os = os(StatusOs.AGUARDANDO_RETIRADA);
        travar(os);
        assertThrows(NegocioException.class,
                () -> fechamento.encerrar(49L, ator(Perfis.GERENTE)));
    }
}
