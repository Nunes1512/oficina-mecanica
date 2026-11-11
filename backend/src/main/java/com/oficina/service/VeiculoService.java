package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.form.VeiculoForm;
import com.oficina.model.Cliente;
import com.oficina.model.StatusVeiculo;
import com.oficina.model.TimelineVeiculo;
import com.oficina.model.Veiculo;
import com.oficina.repository.ClienteRepository;
import com.oficina.repository.OrdemServicoRepository;
import com.oficina.repository.TimelineVeiculoRepository;
import com.oficina.repository.VeiculoRepository;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.util.Textos;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE','ASSISTENTE_ADMINISTRATIVO')")
public class VeiculoService {
    private static final String GESTAO = "hasAnyRole('ADMIN_MASTER','GERENTE')";
    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;
    private final TimelineVeiculoRepository timeline;
    private final OrdemServicoRepository ordens;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<Veiculo> listar(String busca, boolean incluirEntregues) {
        String termo = Textos.limpar(busca);
        return termo == null ? veiculos.findAllByOrderByDataEntradaDesc()
                : veiculos.findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCaseOrderByDataEntradaDesc(termo, termo);
    }
    @Transactional(readOnly = true)
    public Veiculo buscar(Long id) {
        return veiculos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado."));
    }
    @Transactional(readOnly = true)
    public List<TimelineVeiculo> timeline(Long id) { return timeline.findByVeiculoIdOrderByDataAscIdAsc(id); }

    public Veiculo criar(VeiculoForm form) {
        String placa = Textos.placa(form.getPlaca());
        if (veiculos.existsByPlaca(placa)) throw new NegocioException("Já existe veículo com esta placa.");
        Veiculo veiculo = new Veiculo();
        aplicar(veiculo, form, placa);
        // É o instante do cadastro; não é um evento de entrada física na oficina.
        veiculo.setDataEntrada(LocalDateTime.now());
        veiculo.setStatus(StatusVeiculo.AGUARDANDO_ANALISE);
        return veiculos.save(veiculo);
    }
    public Veiculo atualizar(Long id, VeiculoForm form) {
        Veiculo veiculo = buscar(id);
        String placa = Textos.placa(form.getPlaca());
        if (veiculos.existsByPlacaAndIdNot(placa, id))
            throw new NegocioException("Já existe outro veículo com esta placa.");
        // A OS referencia o cadastro do veículo: trocar o dono apagaria a vinculação histórica.
        if (!veiculo.getCliente().getId().equals(form.getClienteId()) && ordens.countByVeiculoId(id) > 0)
            throw new NegocioException("Veículo com histórico de OS não pode trocar de proprietário neste cadastro. Preserve o histórico.");
        aplicar(veiculo, form, placa);
        return veiculo;
    }
    @PreAuthorize(GESTAO)
    public Veiculo alterarStatus(Long id, StatusVeiculo novo, UsuarioPrincipal ator) {
        Veiculo veiculo = buscar(id);
        StatusVeiculo atual = veiculo.getStatus();
        FluxoStatusVeiculo.validar(atual, novo, true);
        veiculo.setStatus(novo);
        auditoria.registrar(ator, "STATUS", "veiculo", id,
                veiculo.getPlaca() + ": " + atual.getRotulo() + " para " + novo.getRotulo());
        return veiculo;
    }
    @Transactional(readOnly = true)
    public StatusVeiculo proximoDisponivel(Veiculo v, UsuarioPrincipal ator) {
        return ator != null && ator.isGerenteOuAdmin() ? FluxoStatusVeiculo.proximoPara(v.getStatus(), true) : null;
    }
    @Transactional(readOnly = true)
    public StatusVeiculo anteriorDisponivel(Veiculo v, UsuarioPrincipal ator) {
        return ator != null && ator.isGerenteOuAdmin() ? FluxoStatusVeiculo.anteriorPara(v.getStatus(), true) : null;
    }
    @PreAuthorize(GESTAO)
    public void excluir(Long id, UsuarioPrincipal ator) {
        Veiculo veiculo = buscar(id);
        if (ordens.countByVeiculoId(id) > 0)
            throw new NegocioException("Este veículo possui ordens de serviço e não pode ser excluído.");
        veiculos.delete(veiculo);
        auditoria.registrar(ator, "EXCLUIR", "veiculo", id, "Veículo excluído: " + veiculo.getPlaca());
    }
    private void aplicar(Veiculo v, VeiculoForm form, String placa) {
        Cliente dono = clientes.findById(form.getClienteId())
                .orElseThrow(() -> new NegocioException("Cliente não encontrado."));
        v.setMarca(form.getMarca().trim());
        v.setModelo(form.getModelo().trim());
        v.setAno(form.getAno());
        v.setCor(Textos.limpar(form.getCor()));
        v.setPlaca(placa);
        v.setQuilometragem(form.getQuilometragem());
        v.setCliente(dono);
        // Não alterar previsao_entrega, data_entrada ou data_entrega durante a edição cadastral.
    }
}
