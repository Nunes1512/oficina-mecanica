package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.model.*;
import com.oficina.repository.*;
import com.oficina.security.UsuarioPrincipal;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Fluxo pós-execução: conclusão técnica -> pagamento -> retirada -> encerramento. */
@Service
@RequiredArgsConstructor
@Transactional
public class FechamentoOsService {
    private static final long LIMITE_ARQUIVO = 3L * 1024 * 1024;

    private final OrdemServicoRepository ordens;
    private final PagamentoOsRepository pagamentos;
    private final RetiradaOsRepository retiradas;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public record Comprovante(String nome, String mime, byte[] dados) { }

    public OrdemServico liberarPagamento(Long osId, UsuarioPrincipal ator) {
        exigirGestao(ator);
        OrdemServico os = bloquear(osId);
        if (os.getStatus() != StatusOs.CONCLUIDA)
            throw new NegocioException("A OS precisa estar tecnicamente concluída para liberar o pagamento.");
        if (!ordens.temOrcamentoAprovado(osId, DecisaoCliente.APROVADO))
            throw new NegocioException("OS histórica sem orçamento aprovado: não é possível liberar pagamento pelo fluxo novo.");
        if (pagamentos.existsByOsId(osId))
            throw new NegocioException("Esta OS já possui pagamento registrado.");
        os.setStatus(StatusOs.AGUARDANDO_PAGAMENTO);
        auditoria.registrar(ator, "LIBERAR_PAGAMENTO", "ordem_servico", osId,
                "Execução validada; pagamento liberado");
        return os;
    }

    public PagamentoOs registrarPagamento(Long osId, FormaPagamentoOs forma, String observacao,
                                         MultipartFile arquivo, UsuarioPrincipal ator) {
        exigirRecepcaoOuGestao(ator);
        OrdemServico os = bloquear(osId);
        if (os.getStatus() != StatusOs.AGUARDANDO_PAGAMENTO)
            throw new NegocioException("A OS não está aguardando pagamento.");
        if (pagamentos.existsByOsId(osId))
            throw new NegocioException("Pagamento já registrado: não é permitido cobrar duas vezes.");
        if (forma == null) throw new NegocioException("Informe a forma de pagamento.");
        validarObservacao(observacao);
        if (os.getValorTotal() == null || os.getValorTotal().signum() < 0)
            throw new NegocioException("Total da OS inválido. Confira os valores da execução.");
        Comprovante comprovante = validarComprovante(arquivo, forma);
        PagamentoOs pagamento = new PagamentoOs();
        pagamento.setOs(os);
        pagamento.setValor(os.getValorTotal()); // valor do banco, nunca enviado pelo navegador
        pagamento.setFormaPagamento(forma);
        pagamento.setPagoEm(LocalDateTime.now());
        pagamento.setRegistradoPorUsuario(usuarios.getReferenceById(ator.getId()));
        pagamento.setObservacao(normalizar(observacao));
        if (comprovante != null) {
            pagamento.setComprovanteNome(comprovante.nome());
            pagamento.setComprovanteMime(comprovante.mime());
            pagamento.setComprovanteDados(comprovante.dados());
        }
        pagamento = pagamentos.saveAndFlush(pagamento);
        os.setStatus(StatusOs.AGUARDANDO_RETIRADA);
        auditoria.registrar(ator, "REGISTRAR_PAGAMENTO", "ordem_servico", osId,
                "Pagamento integral de R$ " + pagamento.getValor() + ", forma " + forma);
        return pagamento;
    }

    public RetiradaOs registrarRetirada(Long osId, String nomeRetirante, String observacao,
                                       UsuarioPrincipal ator) {
        exigirRecepcaoOuGestao(ator);
        OrdemServico os = bloquear(osId);
        if (os.getStatus() != StatusOs.AGUARDANDO_RETIRADA)
            throw new NegocioException("A OS não está aguardando retirada do veículo.");
        if (!pagamentos.existsByOsId(osId))
            throw new NegocioException("Registre o pagamento antes de liberar o veículo.");
        if (retiradas.existsByOsId(osId))
            throw new NegocioException("A retirada deste veículo já foi registrada.");
        if (nomeRetirante == null || nomeRetirante.isBlank() || nomeRetirante.trim().length() > 120)
            throw new NegocioException("Informe quem retirou o veículo (até 120 caracteres).");
        validarObservacao(observacao);
        RetiradaOs retirada = new RetiradaOs();
        retirada.setOs(os);
        retirada.setNomeRetirante(nomeRetirante.trim());
        retirada.setObservacao(normalizar(observacao));
        retirada.setRetiradoEm(LocalDateTime.now());
        retirada.setRegistradoPorUsuario(usuarios.getReferenceById(ator.getId()));
        retirada = retiradas.saveAndFlush(retirada);
        // O status do veículo já existe no modelo legado; a mudança registra a entrega
        // na timeline e a data_entrega pelo trigger V6, sem manipular a tabela diretamente.
        os.getVeiculo().setStatus(StatusVeiculo.ENTREGUE);
        os.setStatus(StatusOs.VEICULO_RETIRADO);
        auditoria.registrar(ator, "REGISTRAR_RETIRADA", "ordem_servico", osId,
                "Veículo retirado; registro #" + retirada.getId());
        return retirada;
    }

    public OrdemServico encerrar(Long osId, UsuarioPrincipal ator) {
        exigirGestao(ator);
        OrdemServico os = bloquear(osId);
        if (os.getStatus() != StatusOs.VEICULO_RETIRADO)
            throw new NegocioException("A OS só pode ser encerrada após a retirada do veículo.");
        if (!pagamentos.existsByOsId(osId) || !retiradas.existsByOsId(osId))
            throw new NegocioException("Pagamento e retirada precisam estar registrados.");
        os.setStatus(StatusOs.ENCERRADA);
        auditoria.registrar(ator, "ENCERRAR", "ordem_servico", osId,
                "OS encerrada após pagamento e retirada");
        return os;
    }

    @Transactional(readOnly = true)
    public Optional<PagamentoOs> consultarPagamento(Long osId, UsuarioPrincipal ator) {
        exigirRecepcaoOuGestao(ator);
        validarLeitura(osId, ator);
        return pagamentos.findByOsId(osId);
    }

    @Transactional(readOnly = true)
    public Optional<RetiradaOs> consultarRetirada(Long osId, UsuarioPrincipal ator) {
        exigirRecepcaoOuGestao(ator);
        validarLeitura(osId, ator);
        return retiradas.findByOsId(osId);
    }

    @Transactional(readOnly = true)
    public Comprovante baixarComprovante(Long osId, UsuarioPrincipal ator) {
        exigirRecepcaoOuGestao(ator);
        validarLeitura(osId, ator);
        PagamentoOs pagamento = pagamentos.findByOsId(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado."));
        if (pagamento.getComprovanteDados() == null || pagamento.getComprovanteDados().length == 0)
            throw new RecursoNaoEncontradoException("Este pagamento não possui comprovante.");
        return new Comprovante(pagamento.getComprovanteNome(), pagamento.getComprovanteMime(),
                pagamento.getComprovanteDados());
    }

    private OrdemServico bloquear(Long id) {
        return ordens.bloquearParaExecucao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
    }

    private void validarLeitura(Long osId, UsuarioPrincipal ator) {
        exigirAutenticado(ator);
        OrdemServico os = ordens.findById(osId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        if (ator.isGerenteOuAdmin() || ator.isAssistente()) return;
        if (ator.isMecanico() && os.getFuncionario() != null && os.getFuncionario().getUsuario() != null
                && os.getFuncionario().getUsuario().getId().equals(ator.getId())) return;
        throw new AccessDeniedException("Sem permissão para consultar o pagamento desta OS.");
    }

    private void exigirAutenticado(UsuarioPrincipal ator) {
        if (ator == null) throw new AccessDeniedException("É necessário estar autenticado.");
    }

    private void exigirGestao(UsuarioPrincipal ator) {
        exigirAutenticado(ator);
        if (!ator.isGerenteOuAdmin()) throw new AccessDeniedException("Ação exclusiva de gerente ou Admin Master.");
    }

    private void exigirRecepcaoOuGestao(UsuarioPrincipal ator) {
        exigirAutenticado(ator);
        if (!ator.isGerenteOuAdmin() && !ator.isAssistente())
            throw new AccessDeniedException("Ação exclusiva da recepção ou gestão.");
    }

    private void validarObservacao(String observacao) {
        if (observacao != null && observacao.length() > 500)
            throw new NegocioException("Observação limitada a 500 caracteres.");
    }

    private String normalizar(String observacao) {
        return observacao == null || observacao.isBlank() ? null : observacao.trim();
    }

    /** Não confia em extensão ou Content-Type do cliente: verifica assinatura do arquivo. */
    private Comprovante validarComprovante(MultipartFile arquivo, FormaPagamentoOs forma) {
        if (arquivo == null || arquivo.isEmpty()) {
            if (forma != FormaPagamentoOs.DINHEIRO)
                throw new NegocioException("Anexe o comprovante do pagamento (PDF, PNG ou JPG). Para dinheiro, é opcional.");
            return null;
        }
        if (arquivo.getSize() > LIMITE_ARQUIVO)
            throw new NegocioException("Comprovante maior que 3 MB.");
        try {
            byte[] dados = arquivo.getBytes();
            if (dados.length == 0 || dados.length > LIMITE_ARQUIVO)
                throw new NegocioException("Comprovante vazio ou maior que 3 MB.");
            String mime;
            String extensao;
            if (dados.length >= 5 && dados[0]=='%' && dados[1]=='P' && dados[2]=='D' && dados[3]=='F' && dados[4]=='-') {
                mime = "application/pdf"; extensao = "pdf";
            } else if (dados.length >= 8 && (dados[0]&255)==137 && dados[1]=='P' && dados[2]=='N'
                    && dados[3]=='G' && (dados[4]&255)==13 && (dados[5]&255)==10
                    && (dados[6]&255)==26 && (dados[7]&255)==10) {
                mime = "image/png"; extensao = "png";
            } else if (dados.length >= 3 && (dados[0]&255)==255 && (dados[1]&255)==216
                    && (dados[2]&255)==255) {
                mime = "image/jpeg"; extensao = "jpg";
            } else {
                throw new NegocioException("Formato de comprovante inválido. Use PDF, PNG ou JPG.");
            }
            return new Comprovante("comprovante-os." + extensao, mime, dados);
        } catch (IOException erro) {
            throw new NegocioException("Não foi possível ler o comprovante anexado.");
        }
    }
}
