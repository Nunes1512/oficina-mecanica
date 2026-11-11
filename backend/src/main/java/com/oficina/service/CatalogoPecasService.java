package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.form.EntradaEstoqueForm;
import com.oficina.form.PecaForm;
import com.oficina.model.MovimentacaoEstoque;
import com.oficina.model.Peca;
import com.oficina.model.TipoMovimentacao;
import com.oficina.repository.MovimentacaoEstoqueRepository;
import com.oficina.repository.PecaRepository;
import com.oficina.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de peças e entradas reais de estoque. O preço de venda é apenas uma
 * referência para futuros orçamentos: nunca altera preços de orçamentos antigos.
 */
@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE')")
public class CatalogoPecasService {
    private final PecaRepository pecas;
    private final MovimentacaoEstoqueRepository movimentacoes;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<Peca> listar(String busca) {
        if (busca == null || busca.isBlank()) return pecas.findAllByOrderByNome();
        return pecas.findByNomeContainingIgnoreCaseOrderByNome(busca.trim());
    }

    @Transactional(readOnly = true)
    public Peca buscar(Long id) {
        return pecas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada."));
    }

    public Peca cadastrar(PecaForm form, UsuarioPrincipal ator) {
        validarAtor(ator);
        validarFormulario(form);
        Peca nova = new Peca();
        aplicar(form, nova);
        // peca.quantidade é preenchida pelo DEFAULT 0 do PostgreSQL.
        Peca salva = pecas.saveAndFlush(nova);
        auditoria.registrar(ator, "CADASTRAR_PECA", "peca", salva.getId(),
                "Cadastro da peça, preço de referência definido");
        return salva;
    }

    public Peca atualizar(Long id, PecaForm form, UsuarioPrincipal ator) {
        validarAtor(ator);
        validarFormulario(form);
        Peca atual = buscar(id);
        BigDecimal precoAnterior = atual.getValorUnitario();
        aplicar(form, atual);
        auditoria.registrar(ator, "ALTERAR_PECA", "peca", id,
                "Preço de referência anterior: " + precoAnterior + "; novo: " + atual.getValorUnitario());
        return atual;
    }

    /**
     * Entrada de estoque em transacao: trava a peça, registra movimento/custo e
     * incrementa saldo. Não altera o preço de venda nem orçamentos existentes.
     */
    public void registrarEntrada(Long id, EntradaEstoqueForm form, UsuarioPrincipal ator) {
        validarAtor(ator);
        if (form == null || form.getQuantidade() == null || form.getQuantidade() <= 0
                || form.getQuantidade() > 1000000 || !precoValido(form.getCustoUnitario())) {
            throw new NegocioException("Informe quantidade positiva e custo unitário válido.");
        }
        if (form.getObservacao() != null && form.getObservacao().length() > 200) {
            throw new NegocioException("Observação limitada a 200 caracteres.");
        }
        Peca peca = pecas.buscarParaAtualizar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada."));
        int saldo = peca.getQuantidade() == null ? 0 : peca.getQuantidade();
        if ((long) saldo + form.getQuantidade() > Integer.MAX_VALUE) {
            throw new NegocioException("A entrada excede o limite do estoque.");
        }
        MovimentacaoEstoque movimento = new MovimentacaoEstoque();
        movimento.setPeca(peca);
        movimento.setTipo(TipoMovimentacao.ENTRADA);
        movimento.setQuantidade(form.getQuantidade());
        movimento.setCustoUnitario(form.getCustoUnitario());
        String obs = form.getObservacao();
        movimento.setObservacao(obs == null || obs.isBlank()
                ? "Entrada manual registrada no módulo Peças"
                : obs.trim());
        movimentacoes.save(movimento);
        if (pecas.somarQuantidade(id, form.getQuantidade()) != 1) {
            throw new NegocioException("Não foi possível registrar a entrada de estoque.");
        }
        auditoria.registrar(ator, "ENTRADA_ESTOQUE", "peca", id,
                "Entrada de " + form.getQuantidade() + " unidade(s) registrada");
    }

    private static void aplicar(PecaForm form, Peca peca) {
        peca.setNome(form.getNome().trim());
        peca.setFabricante(form.getFabricante() == null ? null : form.getFabricante().trim());
        peca.setValorUnitario(form.getValorUnitario());
        peca.setEstoqueMinimo(form.getEstoqueMinimo());
    }

    private static void validarFormulario(PecaForm f) {
        if (f == null || f.getNome() == null || f.getNome().isBlank()
                || f.getNome().length() > 120 || (f.getFabricante() != null && f.getFabricante().length() > 80)
                || !precoValido(f.getValorUnitario()) || f.getEstoqueMinimo() == null
                || f.getEstoqueMinimo() < 0 || f.getEstoqueMinimo() > 1000000) {
            throw new NegocioException("Confira os dados da peça e o preço de referência.");
        }
    }

    private static boolean precoValido(BigDecimal valor) {
        return valor != null && valor.signum() >= 0 && valor.scale() <= 2
                && valor.precision() - valor.scale() <= 8;
    }

    private static void validarAtor(UsuarioPrincipal ator) {
        if (ator == null || !ator.isGerenteOuAdmin()) {
            throw new AccessDeniedException("Somente a gestão pode administrar peças.");
        }
    }
}
