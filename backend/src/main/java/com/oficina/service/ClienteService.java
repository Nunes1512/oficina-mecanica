package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.form.ClienteForm;
import com.oficina.model.Cliente;
import com.oficina.repository.ClienteRepository;
import com.oficina.repository.VeiculoRepository;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.util.Textos;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE','ASSISTENTE_ADMINISTRATIVO')")
public class ClienteService {

    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<Cliente> listar(String busca) {
        String termo = Textos.limpar(busca);
        if (termo == null) {
            return clientes.findAllByOrderByNome();
        }
        return clientes.findByNomeContainingIgnoreCaseOrCpfContainingOrderByNome(termo, termo);
    }

    @Transactional(readOnly = true)
    public List<ClienteRepository.ClientesPorMes> clientesPorMes() {
        return clientes.clientesPorMes();
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
    }

    public Cliente criar(ClienteForm form) {
        String cpf = Textos.cpf(form.getCpf());
        if (clientes.existsByCpf(cpf)) {
            throw new NegocioException("Já existe um cliente com este CPF.");
        }
        Cliente cliente = new Cliente();
        aplicar(cliente, form, cpf);
        return clientes.save(cliente);
    }

    public Cliente atualizar(Long id, ClienteForm form) {
        Cliente cliente = buscar(id);
        String cpf = Textos.cpf(form.getCpf());
        if (clientes.existsByCpfAndIdNot(cpf, id)) {
            throw new NegocioException("Já existe outro cliente com este CPF.");
        }
        aplicar(cliente, form, cpf);
        return cliente;
    }

    @PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE')")
    public void excluir(Long id, UsuarioPrincipal ator) {
        Cliente cliente = buscar(id);
        if (veiculos.countByClienteId(id) > 0) {
            throw new NegocioException("Este cliente possui veículos cadastrados e não pode ser excluído.");
        }
        clientes.delete(cliente);
        auditoria.registrar(ator, "EXCLUIR", "cliente", id, "Cliente excluído: " + cliente.getNome());
    }

    private void aplicar(Cliente cliente, ClienteForm form, String cpf) {
        cliente.setNome(form.getNome().trim());
        cliente.setCpf(cpf);
        cliente.setTelefone(Textos.limpar(form.getTelefone()));
        cliente.setEmail(Textos.limpar(form.getEmail()));
        cliente.setEndereco(Textos.limpar(form.getEndereco()));
    }
}
