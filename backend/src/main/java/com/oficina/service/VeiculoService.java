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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VeiculoService {

    private static final String GERENTE_OU_ADMIN = "hasAnyRole('ADMIN_MASTER','GERENTE')";
    private static final String RECEPCAO_OU_GESTAO =
            "hasAnyRole('ADMIN_MASTER','GERENTE','ASSISTENTE_ADMINISTRATIVO')";

    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;
    private final TimelineVeiculoRepository timeline;
    private final OrdemServicoRepository ordens;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<Veiculo> listar(String busca, boolean incluirEntregues) {
        String termo = Textos.limpar(busca);
        List<Veiculo> base = termo == null
                ? veiculos.findAllByOrderByDataEntradaDesc()
                : veiculos.findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCaseOrderByDataEntradaDesc(termo, termo);

        List<Veiculo> fila = new ArrayList<>(base.stream()
                .filter(v -> v.getStatus() != StatusVeiculo.ENTREGUE)
                .sorted(Comparator.comparing(Veiculo::getDataEntrada))
                .toList());
        if (incluirEntregues || termo != null) {
            base.stream().filter(v -> v.getStatus() == StatusVeiculo.ENTREGUE).forEach(fila::add);
        }
        return fila;
    }

    @Transactional(readOnly = true)
    public Veiculo buscar(Long id) {
        return veiculos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<TimelineVeiculo> timeline(Long veiculoId) {
        return timeline.findByVeiculoIdOrderByDataAscIdAsc(veiculoId);
    }

    @PreAuthorize(RECEPCAO_OU_GESTAO)
    public Veiculo criar(VeiculoForm form) {
        String placa = Textos.placa(form.getPlaca());
        if (veiculos.existsByPlaca(placa)) {
            throw new NegocioException("Já existe um veículo com esta placa.");
        }
        Veiculo veiculo = new Veiculo();
        aplicar(veiculo, form, placa);
        veiculo.setStatus(StatusVeiculo.AGUARDANDO_ANALISE);
        return veiculos.save(veiculo);
    }

    @PreAuthorize(RECEPCAO_OU_GESTAO)
    public Veiculo atualizar(Long id, VeiculoForm form) {
        Veiculo veiculo = buscar(id);
        String placa = Textos.placa(form.getPlaca());
        if (veiculos.existsByPlacaAndIdNot(placa, id)) {
            throw new NegocioException("Já existe outro veículo com esta placa.");
        }
        aplicar(veiculo, form, placa);
        return veiculo;
    }

    /** Status geral do veículo: somente a gestão; status de serviço do mecânico ficam na OS. */
    @PreAuthorize(GERENTE_OU_ADMIN)
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
    public StatusVeiculo proximoDisponivel(Veiculo veiculo, UsuarioPrincipal ator) {
        if (ator == null || !ator.isGerenteOuAdmin()) {
            return null;
        }
        return FluxoStatusVeiculo.proximoPara(veiculo.getStatus(), true);
    }

    @Transactional(readOnly = true)
    public StatusVeiculo anteriorDisponivel(Veiculo veiculo, UsuarioPrincipal ator) {
        if (ator == null || !ator.isGerenteOuAdmin()) {
            return null;
        }
        return FluxoStatusVeiculo.anteriorPara(veiculo.getStatus(), true);
    }

    @PreAuthorize(GERENTE_OU_ADMIN)
    public void excluir(Long id, UsuarioPrincipal ator) {
        Veiculo veiculo = buscar(id);
        if (ordens.countByVeiculoId(id) > 0) {
            throw new NegocioException("Este veículo possui ordens de serviço e não pode ser excluído.");
        }
        veiculos.delete(veiculo);
        auditoria.registrar(ator, "EXCLUIR", "veiculo", id, "Veículo excluído: " + veiculo.getPlaca());
    }

    private void aplicar(Veiculo veiculo, VeiculoForm form, String placa) {
        if (form.getDataEntrada() == null) {
            throw new NegocioException("Informe a data de chegada.");
        }
        LocalDate previsao = form.getPrevisaoEntrega();
        if (previsao != null && previsao.isBefore(form.getDataEntrada().toLocalDate())) {
            throw new NegocioException("A previsão de entrega não pode ser anterior à data de chegada.");
        }
        Cliente cliente = clientes.findById(form.getClienteId())
                .orElseThrow(() -> new NegocioException("Cliente não encontrado."));
        veiculo.setMarca(form.getMarca().trim());
        veiculo.setModelo(form.getModelo().trim());
        veiculo.setAno(form.getAno());
        veiculo.setCor(Textos.limpar(form.getCor()));
        veiculo.setPlaca(placa);
        veiculo.setQuilometragem(form.getQuilometragem());
        veiculo.setCliente(cliente);
        veiculo.setDataEntrada(form.getDataEntrada());
        veiculo.setPrevisaoEntrega(previsao == null ? null : previsao.atTime(LocalTime.of(23, 59)));
    }
}
