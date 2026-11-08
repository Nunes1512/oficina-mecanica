package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.form.FuncionarioForm;
import com.oficina.model.Funcionario;
import com.oficina.repository.FuncionarioRepository;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.util.Textos;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Não há exclusão: o funcionário é marcado como INATIVO, para preservar o histórico das OS.
 * O salário só é visto e alterado pelo administrador.
 */
@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE')")
public class FuncionarioService {

    private final FuncionarioRepository funcionarios;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<Funcionario> listar() {
        return funcionarios.findAllByOrderByNome();
    }

    @Transactional(readOnly = true)
    public Funcionario buscar(Long id) {
        return funcionarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado."));
    }

    public Funcionario criar(FuncionarioForm form, UsuarioPrincipal ator) {
        String cpf = Textos.cpf(form.getCpf());
        if (funcionarios.existsByCpf(cpf)) {
            throw new NegocioException("Já existe um funcionário com este CPF.");
        }
        Funcionario funcionario = new Funcionario();
        funcionario.setSalario(BigDecimal.ZERO);
        aplicar(funcionario, form, cpf, ator);
        funcionario = funcionarios.save(funcionario);
        auditoria.registrar(ator, "CRIAR", "funcionario", funcionario.getId(),
                "Funcionário cadastrado: " + funcionario.getNome());
        return funcionario;
    }

    public Funcionario atualizar(Long id, FuncionarioForm form, UsuarioPrincipal ator) {
        Funcionario funcionario = buscar(id);
        String cpf = Textos.cpf(form.getCpf());
        if (funcionarios.existsByCpfAndIdNot(cpf, id)) {
            throw new NegocioException("Já existe outro funcionário com este CPF.");
        }
        aplicar(funcionario, form, cpf, ator);
        auditoria.registrar(ator, "EDITAR", "funcionario", id, "Funcionário alterado: " + funcionario.getNome());
        return funcionario;
    }

    private void aplicar(Funcionario funcionario, FuncionarioForm form, String cpf, UsuarioPrincipal ator) {
        funcionario.setNome(form.getNome().trim());
        funcionario.setCpf(cpf);
        funcionario.setTelefone(Textos.limpar(form.getTelefone()));
        funcionario.setEmail(Textos.limpar(form.getEmail()));
        funcionario.setCargo(Textos.limpar(form.getCargo()));
        funcionario.setStatus(form.getStatus());
        // O salário é dado sensível: só o administrador altera. Mesmo que outro perfil envie o campo, ele é ignorado.
        if (ator != null && ator.isAdmin() && form.getSalario() != null) {
            funcionario.setSalario(form.getSalario());
        }
    }
}
