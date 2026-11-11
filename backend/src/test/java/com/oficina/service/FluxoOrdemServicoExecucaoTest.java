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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class FluxoOrdemServicoExecucaoTest {
    @Mock OrdemServicoRepository ordens;
    @Mock VeiculoRepository veiculos;
    @Mock FuncionarioRepository funcionarios;
    @Mock UsuarioRepository usuarios;
    @Mock PecaRepository pecas;
    @Mock OrcamentoRepository orcamentos;
    @Mock ItemOrcamentoRepository itens;
    @Mock ItemOsRepository itensOs;
    @Mock ExecucaoServicoRepository execucoes;
    @Mock NotificacaoRepository notificacoes;
    @Mock AuditoriaService auditoria;
    @InjectMocks FluxoOrdemServicoService fluxo;

    private UsuarioPrincipal mecanico(Long id) {
        return new UsuarioPrincipal(id, "mecanico@teste.com", "hash", Perfis.MECANICO, true, false);
    }
    private UsuarioPrincipal gerente() {
        return new UsuarioPrincipal(88L, "gerente@teste.com", "hash", Perfis.GERENTE, true, false);
    }
    private OrdemServico os(StatusOs status) {
        OrdemServico os = new OrdemServico();
        os.setId(49L);
        os.setNumero("OS-TESTE");
        os.setStatus(status);
        Usuario user = new Usuario();
        user.setId(7L);
        Funcionario f = new Funcionario();
        f.setId(8L);
        f.setUsuario(user);
        os.setFuncionario(f);
        return os;
    }
    private Orcamento aprovado(OrdemServico os) {
        Orcamento orc = new Orcamento();
        orc.setId(11L);
        orc.setOs(os);
        orc.setVersao(2);
        orc.setAprovadoGerente(true);
        orc.setDecisaoCliente(DecisaoCliente.APROVADO);
        return orc;
    }
    private ItemOrcamento servico(Orcamento orc, long id) {
        ItemOrcamento item = new ItemOrcamento();
        item.setId(id);
        item.setOrcamento(orc);
        item.setTipo(TipoItemOrcamento.SERVICO);
        item.setDescricao("Troca de óleo");
        item.setQuantidade(2);
        item.setValorUnitario(new BigDecimal("50.00"));
        return item;
    }

    @Test
    void mecanicoDesignadoPodeIniciarExecucaoDoOrcamentoAprovado() {
        OrdemServico os = os(StatusOs.AGUARDANDO_EXECUCAO);
        Orcamento orc = aprovado(os);
        ItemOrcamento servico = servico(orc, 33L);
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));
        when(itens.findByOrcamentoIdOrderByIdAsc(11L)).thenReturn(List.of(servico));
        when(execucoes.findByOsIdAndItemOrcamentoId(49L, 33L)).thenReturn(Optional.empty());

        fluxo.iniciarExecucao(49L, mecanico(7L));

        assertEquals(StatusOs.EM_ANDAMENTO, os.getStatus());
        verify(execucoes).save(any(ExecucaoServico.class));
    }

    @Test
    void mecanicoNaoPodeIniciarOsAtribuidaAOutroFuncionario() {
        OrdemServico os = os(StatusOs.AGUARDANDO_EXECUCAO);
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
        assertThrows(AccessDeniedException.class,
                () -> fluxo.iniciarExecucao(49L, mecanico(999L)));
        verifyNoInteractions(execucoes, itensOs);
    }

    @Test
    void naoIniciaSemAprovacaoDoCliente() {
        OrdemServico os = os(StatusOs.AGUARDANDO_EXECUCAO);
        Orcamento orc = aprovado(os);
        orc.setDecisaoCliente(DecisaoCliente.RECUSADO);
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));

        assertThrows(NegocioException.class, () -> fluxo.iniciarExecucao(49L, mecanico(7L)));
        assertEquals(StatusOs.AGUARDANDO_EXECUCAO, os.getStatus());
    }

    @Test
    void atualizaServicoSomenteEmOrdemEQuandoPertenceAoOrcamento() {
        OrdemServico os = os(StatusOs.EM_ANDAMENTO);
        Orcamento orc = aprovado(os);
        ItemOrcamento item = servico(orc, 33L);
        ExecucaoServico e = new ExecucaoServico();
        e.setOs(os);
        e.setItemOrcamento(item);
        e.setStatus(StatusExecucaoServico.PENDENTE);
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));
        when(itens.findById(33L)).thenReturn(Optional.of(item));
        when(execucoes.findByOsIdAndItemOrcamentoId(49L, 33L)).thenReturn(Optional.of(e));

        fluxo.atualizarServico(49L, 33L, StatusExecucaoServico.EM_ANDAMENTO, mecanico(7L));
        assertEquals(StatusExecucaoServico.EM_ANDAMENTO, e.getStatus());
        assertNotNull(e.getIniciadoEm());
        assertThrows(NegocioException.class,
                () -> fluxo.atualizarServico(49L, 33L, StatusExecucaoServico.PENDENTE, mecanico(7L)));
    }

    @Test
    void concluirExecucaoExigeTodosServicosConcluidos() {
        OrdemServico os = os(StatusOs.EM_ANDAMENTO);
        Orcamento orc = aprovado(os);
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));
        when(itens.findByOrcamentoIdOrderByIdAsc(11L)).thenReturn(List.of(servico(orc, 33L)));
        when(execucoes.findByOsIdOrderById(49L)).thenReturn(List.of());

        assertThrows(NegocioException.class, () -> fluxo.concluirExecucao(49L, mecanico(7L)));
        assertEquals(StatusOs.EM_ANDAMENTO, os.getStatus());
    }

    @Test
    void concluirExecucaoRegistraMaoDeObraDoOrcamentoAprovado() {
        OrdemServico os = os(StatusOs.EM_ANDAMENTO);
        Orcamento orc = aprovado(os);
        ItemOrcamento item = servico(orc, 33L);
        ExecucaoServico e = new ExecucaoServico();
        e.setOs(os);
        e.setItemOrcamento(item);
        e.setStatus(StatusExecucaoServico.CONCLUIDO);
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));
        when(itens.findByOrcamentoIdOrderByIdAsc(11L)).thenReturn(List.of(item));
        when(execucoes.findByOsIdOrderById(49L)).thenReturn(List.of(e));
        when(itensOs.findByOsIdOrderById(49L)).thenReturn(List.of());

        fluxo.concluirExecucao(49L, mecanico(7L));

        assertEquals(StatusOs.CONCLUIDA, os.getStatus());
        assertEquals(new BigDecimal("100.00"), os.getValorMaoObra());
    }

    @Test
    void registrarPecasNaoDebitaEstoqueDuasVezes() {
        OrdemServico os = os(StatusOs.EM_ANDAMENTO);
        Orcamento orc = aprovado(os);
        Peca peca = new Peca();
        peca.setId(12L);
        peca.setNome("Filtro de óleo");
        peca.setQuantidade(8);
        ItemOrcamento item = new ItemOrcamento();
        item.setOrcamento(orc);
        item.setTipo(TipoItemOrcamento.PECA);
        item.setPeca(peca);
        item.setQuantidade(2);
        item.setValorUnitario(new BigDecimal("25.00"));
        ItemOs consumido = new ItemOs();
        consumido.setPeca(peca);
        consumido.setQuantidade(2);
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));
        when(itens.findByOrcamentoIdOrderByIdAsc(11L)).thenReturn(List.of(item));
        when(itensOs.findByOsIdOrderById(49L)).thenReturn(List.of(consumido));

        fluxo.registrarUsoPecas(49L, Map.of(12L, 0), mecanico(7L));

        verify(itensOs, never()).saveAndFlush(any());
    }
    @Test
    void consumoParcialBaixaSomenteQuantidadeDeclarada() {
        OrdemServico os = os(StatusOs.EM_ANDAMENTO);
        Orcamento orc = aprovado(os);
        Peca peca = new Peca();
        peca.setId(12L);
        peca.setNome("Filtro");
        peca.setQuantidade(8);
        ItemOrcamento item = new ItemOrcamento();
        item.setTipo(TipoItemOrcamento.PECA);
        item.setOrcamento(orc);
        item.setPeca(peca);
        item.setQuantidade(3);
        item.setValorUnitario(new BigDecimal("20.00"));
        when(ordens.bloquearParaExecucao(49L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(49L)).thenReturn(Optional.of(orc));
        when(itens.findByOrcamentoIdOrderByIdAsc(11L)).thenReturn(List.of(item));
        when(itensOs.findByOsIdOrderById(49L)).thenReturn(List.of());
        when(pecas.findById(12L)).thenReturn(Optional.of(peca));
        when(pecas.getReferenceById(12L)).thenReturn(peca);
        fluxo.registrarUsoPecas(49L, Map.of(12L, 1), mecanico(7L));
        org.mockito.ArgumentCaptor<ItemOs> argumento = org.mockito.ArgumentCaptor.forClass(ItemOs.class);
        verify(itensOs).saveAndFlush(argumento.capture());
        assertEquals(1, argumento.getValue().getQuantidade());
    }

}
