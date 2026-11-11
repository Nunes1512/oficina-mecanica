package com.oficina.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.oficina.exception.NegocioException;
import com.oficina.model.Funcionario;
import com.oficina.model.Orcamento;
import com.oficina.model.OrdemServico;
import com.oficina.model.StatusOs;
import com.oficina.model.TipoItemOrcamento;
import com.oficina.model.Usuario;
import com.oficina.model.Veiculo;
import com.oficina.repository.FuncionarioRepository;
import com.oficina.repository.ItemOrcamentoRepository;
import com.oficina.repository.NotificacaoRepository;
import com.oficina.repository.OrcamentoRepository;
import com.oficina.repository.OrdemServicoRepository;
import com.oficina.repository.PecaRepository;
import com.oficina.repository.UsuarioRepository;
import com.oficina.repository.VeiculoRepository;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class FluxoOrdemServicoServiceTest {
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

    private UsuarioPrincipal ator(Long id, String perfil) {
        return new UsuarioPrincipal(id, "teste@oficina.com", "hash", perfil, true, false);
    }

    @Test
    void recepcaoAbreOrdemAguardandoAvaliacao() {
        Veiculo veiculo = new Veiculo();
        veiculo.setId(9L);
        when(veiculos.bloquearParaAbertura(9L)).thenReturn(Optional.of(veiculo));
        when(ordens.save(any(OrdemServico.class))).thenAnswer(invocacao -> {
            OrdemServico os = invocacao.getArgument(0);
            os.setId(11L);
            return os;
        });
        OrdemServico os = fluxo.abrir(9L, "Ruído ao frear", ator(2L, Perfis.ASSISTENTE_ADMINISTRATIVO));
        assertEquals(StatusOs.AGUARDANDO_AVALIACAO, os.getStatus());
        assertEquals(Long.valueOf(9L), os.getVeiculo().getId());
        assertTrue(os.getNumero().startsWith("OS-"));
        assertEquals(null, veiculo.getDataEntrega());
        verifyNoInteractions(itens, pecas);
    }

    @Test
    void mecanicoNaoPodeAcessarOrdemDeOutroMecanico() {
        Usuario responsavel = new Usuario();
        responsavel.setId(22L);
        Funcionario funcionario = new Funcionario();
        funcionario.setUsuario(responsavel);
        OrdemServico os = new OrdemServico();
        os.setId(7L);
        os.setFuncionario(funcionario);
        when(ordens.findById(7L)).thenReturn(Optional.of(os));
        assertThrows(AccessDeniedException.class,
                () -> fluxo.buscar(7L, ator(33L, Perfis.MECANICO)));
    }

    @Test
    void mecanicoNaoPodeDefinirPrecos() {
        assertThrows(AccessDeniedException.class,
                () -> fluxo.precificar(1L, 2L, List.of(
                        new FluxoOrdemServicoService.PrecoItem(4L, BigDecimal.TEN)),
                        ator(33L, Perfis.MECANICO)));
    }

    @Test
    void avaliacaoCriaOrcamentoSemMovimentarEstoque() {
        Usuario usuario = new Usuario();
        usuario.setId(33L);
        Funcionario mecanico = new Funcionario();
        mecanico.setUsuario(usuario);
        OrdemServico os = new OrdemServico();
        os.setId(5L);
        os.setFuncionario(mecanico);
        os.setStatus(StatusOs.EM_AVALIACAO);
        when(ordens.findById(5L)).thenReturn(Optional.of(os));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(5L)).thenReturn(Optional.empty());
        when(orcamentos.save(any(Orcamento.class))).thenAnswer(invocacao -> {
            Orcamento orcamento = invocacao.getArgument(0);
            orcamento.setId(4L);
            return orcamento;
        });
        Orcamento resultado = fluxo.registrarAvaliacao(5L,
                List.of(new FluxoOrdemServicoService.ItemAvaliacao(
                        TipoItemOrcamento.SERVICO, "Troca do filtro", null, 1)),
                ator(33L, Perfis.MECANICO));
        assertEquals(Integer.valueOf(1), resultado.getVersao());
        assertEquals(StatusOs.AGUARDANDO_PRECIFICACAO, os.getStatus());
        verifyNoInteractions(pecas, notificacoes);
    }

    @Test
    void gestorNaoPodeAprovarOrcamentoSemEtapaCorreta() {
        OrdemServico os = new OrdemServico();
        os.setId(1L);
        os.setStatus(StatusOs.EM_AVALIACAO);
        Orcamento orcamento = new Orcamento();
        orcamento.setId(2L);
        orcamento.setOs(os);
        when(ordens.findById(1L)).thenReturn(Optional.of(os));
        when(orcamentos.findById(2L)).thenReturn(Optional.of(orcamento));
        when(orcamentos.findTopByOsIdOrderByVersaoDesc(1L)).thenReturn(Optional.of(orcamento));
        assertThrows(NegocioException.class,
                () -> fluxo.aprovarGerente(1L, 2L, ator(1L, Perfis.GERENTE)));
    }
    @Test
    void impedeAberturaSeVeiculoJaTemOsAtiva() {
        Veiculo veiculo = new Veiculo();
        veiculo.setId(9L);
        when(veiculos.bloquearParaAbertura(9L)).thenReturn(Optional.of(veiculo));
        when(ordens.existeAtivaParaVeiculo(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(StatusOs.CONCLUIDA))).thenReturn(true);
        assertThrows(NegocioException.class,
                () -> fluxo.abrir(9L, "Troca de pastilhas", ator(2L, Perfis.ASSISTENTE_ADMINISTRATIVO)));
        assertEquals(null, veiculo.getDataEntrega());
    }

}
