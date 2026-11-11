package com.oficina.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.oficina.exception.NegocioException;
import com.oficina.model.*;
import com.oficina.repository.*;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class FluxoOrdemServicoCorrecoesTest {
    @Mock OrdemServicoRepository ordens;
    @Mock VeiculoRepository veiculos;
    @Mock FuncionarioRepository funcionarios;
    @Mock UsuarioRepository usuarios;
    @Mock PecaRepository pecas;
    @Mock OrcamentoRepository orcamentos;
    @Mock ItemOrcamentoRepository itens;
    @Mock NotificacaoRepository notificacoes;
    @Mock AuditoriaService auditoria;
    @InjectMocks FluxoOrdemServicoService fluxo;

    private UsuarioPrincipal ator(String role) {
        return new UsuarioPrincipal(99L, "teste@oficina.com", "hash", role, true, false);
    }

    private OrdemServico os(Long id, StatusOs status) {
        Veiculo v = new Veiculo();
        v.setId(7L);
        v.setPrevisaoEntrega(LocalDate.now().plusDays(3).atTime(23, 59));
        OrdemServico os = new OrdemServico();
        os.setId(id);
        os.setVeiculo(v);
        os.setStatus(status);
        os.setProblema("Barulho ao ligar");
        return os;
    }

    private Orcamento orcamento(OrdemServico os, DecisaoCliente decisao) {
        Orcamento orcamento = new Orcamento();
        orcamento.setId(18L);
        orcamento.setOs(os);
        orcamento.setVersao(1);
        orcamento.setDecisaoCliente(decisao);
        orcamento.setAprovadoGerente(true);
        return orcamento;
    }

    @Test
    void recusaDoClienteRetornaParaRevisaoPreservandoVersaoAnterior() {
        OrdemServico os = os(49L, StatusOs.AGUARDANDO_CLIENTE);
        Orcamento orc = orcamento(os, DecisaoCliente.PENDENTE);
        Funcao perfil = new Funcao();
        perfil.setNome(Perfis.ASSISTENTE_ADMINISTRATIVO);
        Usuario contato = new Usuario();
        contato.setId(3L);
        contato.setFuncao(perfil);
        when(ordens.findById(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findById(18L)).thenReturn(Optional.of(orc));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));
        when(usuarios.findById(3L)).thenReturn(Optional.of(contato));
        when(usuarios.getReferenceById(99L)).thenReturn(contato);

        fluxo.registrarRespostaCliente(49L, 18L, DecisaoCliente.RECUSADO, 3L,
                "Cliente deseja renegociar", ator(Perfis.ASSISTENTE_ADMINISTRATIVO));

        assertEquals(DecisaoCliente.RECUSADO, orc.getDecisaoCliente());
        assertEquals(StatusOs.REVISAO_ORCAMENTO, os.getStatus());
        assertNull(os.getVeiculo().getPrevisaoEntrega());
        verify(orcamentos, never()).delete(any());
    }

    @Test
    void reabreRecusaAntigaSemModificarDecisaoHistorica() {
        OrdemServico os = os(49L, StatusOs.ORCAMENTO_RECUSADO);
        Orcamento orc = orcamento(os, DecisaoCliente.RECUSADO);
        when(ordens.findById(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));

        fluxo.retomarOrcamentoRecusado(49L, ator(Perfis.GERENTE));

        assertEquals(StatusOs.REVISAO_ORCAMENTO, os.getStatus());
        assertEquals(DecisaoCliente.RECUSADO, orc.getDecisaoCliente());
    }

    @Test
    void adminRetornaAoDiagnosticoSemAlterarOrcamentoJaRespondido() {
        OrdemServico os = os(49L, StatusOs.ORCAMENTO_RECUSADO);
        Orcamento orc = orcamento(os, DecisaoCliente.RECUSADO);
        when(ordens.findById(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));

        fluxo.adminRetornarEtapa(49L, FluxoOrdemServicoService.EtapaCorrecao.PRECIFICACAO,
                "Proposta recusada, corrigir itens", ator(Perfis.ADMIN_MASTER));

        assertEquals(StatusOs.REVISAO_ORCAMENTO, os.getStatus());
        assertEquals(DecisaoCliente.RECUSADO, orc.getDecisaoCliente());
        assertTrue(orc.isAprovadoGerente()); // aprovação anterior não é apagada
    }

    @Test
    void gerenteNaoPodeUsarRetornoExclusivoAdmin() {
        assertThrows(AccessDeniedException.class,
                () -> fluxo.adminRetornarEtapa(49L, FluxoOrdemServicoService.EtapaCorrecao.ABERTURA,
                        "Corrigir cadastro", ator(Perfis.GERENTE)));
        verifyNoInteractions(ordens);
    }

    @Test
    void naoPermiteDesfazerEtapaDePagamento() {
        OrdemServico os = os(49L, StatusOs.AGUARDANDO_PAGAMENTO);
        when(ordens.findById(49L)).thenReturn(Optional.of(os));
        assertThrows(NegocioException.class,
                () -> fluxo.adminRetornarEtapa(49L, FluxoOrdemServicoService.EtapaCorrecao.AVALIACAO,
                        "Tentativa de retorno", ator(Perfis.ADMIN_MASTER)));
    }
}
