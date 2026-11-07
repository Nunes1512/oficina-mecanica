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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultar é liberado a qualquer usuário logado (o funcionário precisa ver os veículos);
 * cadastrar, alterar e excluir é só do gerente e do administrador.
 * A timeline é escrita pelos triggers do banco: aqui só lemos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VeiculoService {

    private static final String GERENTE_OU_ADMIN = "hasAnyRole('ADMIN_MASTER','GERENTE')";

    private final VeiculoRepository veiculos;
    private final ClienteRepository clientes;
    private final TimelineVeiculoRepository timeline;
    private final OrdemServicoRepository ordens;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<Veiculo> listar(String busca) {
        String termo = Textos.limpar(busca);
        if (termo == null) {
            return veiculos.findAllByOrderByDataEntradaDesc();
        }
        return veiculos.findByPlacaContainingIgnoreCaseOrModeloContainingIgnoreCaseOrderByDataEntradaDesc(termo, termo);
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

    @PreAuthorize(GERENTE_OU_ADMIN)
    public Veiculo criar(VeiculoForm form) {
        String placa = Textos.placa(form.getPlaca());
        if (veiculos.existsByPlaca(placa)) {
            throw new NegocioException("Já existe um veículo com esta placa.");
        }
        Veiculo veiculo = new Veiculo();
        aplicar(veiculo, form, placa);
        veiculo.setDataEntrada(LocalDateTime.now());
        veiculo.setStatus(StatusVeiculo.AGUARDANDO_ANALISE);
        return veiculos.save(veiculo);
    }

    @PreAuthorize(GERENTE_OU_ADMIN)
    public Veiculo atualizar(Long id, VeiculoForm form) {
        Veiculo veiculo = buscar(id);
        String placa = Textos.placa(form.getPlaca());
        if (veiculos.existsByPlacaAndIdNot(placa, id)) {
            throw new NegocioException("Já existe outro veículo com esta placa.");
        }
        aplicar(veiculo, form, placa);
        if (form.getStatus() != null) {
            veiculo.setStatus(form.getStatus());
        }
        return veiculo;
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
        Cliente cliente = clientes.findById(form.getClienteId())
                .orElseThrow(() -> new NegocioException("Cliente não encontrado."));
        veiculo.setMarca(form.getMarca().trim());
        veiculo.setModelo(form.getModelo().trim());
        veiculo.setAno(form.getAno());
        veiculo.setCor(Textos.limpar(form.getCor()));
        veiculo.setPlaca(placa);
        veiculo.setQuilometragem(form.getQuilometragem());
        veiculo.setCliente(cliente);
        LocalDate previsao = form.getPrevisaoEntrega();
        veiculo.setPrevisaoEntrega(previsao == null ? null : previsao.atTime(LocalTime.of(23, 59)));
    }
}
