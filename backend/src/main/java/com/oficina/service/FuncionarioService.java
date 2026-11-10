package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.form.FuncionarioForm;
import com.oficina.model.Funcao;
import com.oficina.model.Funcionario;
import com.oficina.model.Usuario;
import com.oficina.repository.FuncaoRepository;
import com.oficina.repository.FuncionarioRepository;
import com.oficina.repository.UsuarioRepository;
import com.oficina.security.PasswordPolicy;
import com.oficina.security.Perfis;
import com.oficina.security.UsuarioPrincipal;
import com.oficina.util.Textos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Funcionários são inativados, não excluídos, para preservar o histórico das OS. */
@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasAnyRole('ADMIN_MASTER','GERENTE')")
public class FuncionarioService {

    private final FuncionarioRepository funcionarios;
    private final UsuarioRepository usuarios;
    private final FuncaoRepository funcoes;
    private final PasswordEncoder encoder;
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
        // Os dados de login preenchidos devem ser reconhecidos mesmo se o checkbox
        // não chegar marcado. Isso evita cadastrar funcionário sem criar sua conta.
        boolean criarAcesso = solicitouCriacaoUsuario(form);
        if (criarAcesso && (ator == null || !ator.isAdmin())) {
            throw new NegocioException("Somente o Admin Master pode criar usuários de acesso.");
        }

        String cpf = Textos.cpf(form.getCpf());
        if (funcionarios.existsByCpf(cpf)) {
            throw new NegocioException("Já existe um funcionário com este CPF.");
        }

        Usuario usuario = criarAcesso ? prepararNovoUsuario(form) : null;

        Funcionario funcionario = new Funcionario();
        // Compatibilidade com a coluna salario NOT NULL da base existente.
        // Não há mais leitura ou alteração de remuneração neste módulo.
        funcionario.setSalario(BigDecimal.ZERO);
        aplicar(funcionario, form, cpf);
        if (usuario != null) {
            funcionario.setUsuario(usuarios.save(usuario));
        }
        funcionario = funcionarios.save(funcionario);
        auditoria.registrar(ator, "CRIAR", "funcionario", funcionario.getId(),
                "Funcionário cadastrado: " + funcionario.getNome());
        if (usuario != null) {
            auditoria.registrar(ator, "CRIAR", "usuario", usuario.getId(),
                    "Acesso criado para o funcionário " + funcionario.getId());
        }
        return funcionario;
    }

    public Funcionario atualizar(Long id, FuncionarioForm form, UsuarioPrincipal ator) {
        // Permite criar login ao editar funcionário sem conta, somente pelo Admin Master.
        boolean criarAcesso = solicitouCriacaoUsuario(form);
        if (criarAcesso && (ator == null || !ator.isAdmin())) {
            throw new NegocioException("Somente o Admin Master pode criar usuários de acesso.");
        }
        Funcionario funcionario = buscar(id);
        if (criarAcesso && funcionario.getUsuario() != null) {
            throw new NegocioException("Este funcionário já possui um usuário vinculado.");
        }
        String cpf = Textos.cpf(form.getCpf());
        if (funcionarios.existsByCpfAndIdNot(cpf, id)) {
            throw new NegocioException("Já existe outro funcionário com este CPF.");
        }
        Usuario usuario = criarAcesso ? prepararNovoUsuario(form) : null;
        aplicar(funcionario, form, cpf);
        if (usuario != null) {
            funcionario.setUsuario(usuarios.save(usuario));
        }
        funcionarios.save(funcionario);
        auditoria.registrar(ator, "EDITAR", "funcionario", id,
                "Funcionário alterado: " + funcionario.getNome());
        if (usuario != null) {
            auditoria.registrar(ator, "CRIAR", "usuario", usuario.getId(),
                    "Acesso criado para o funcionário " + id);
        }
        return funcionario;
    }

    /** A presença de quaisquer dados de acesso equivale a solicitar um usuário. */
    private boolean solicitouCriacaoUsuario(FuncionarioForm form) {
        return form.isCriarUsuario()
                || temTexto(form.getEmailUsuario())
                || temTexto(form.getSenhaInicial())
                || temTexto(form.getPerfilUsuario());
    }

    private boolean temTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private Usuario prepararNovoUsuario(FuncionarioForm form) {
        String email = Textos.limpar(form.getEmailUsuario());
        if (email == null || email.isBlank()) {
            throw new NegocioException("Informe o e-mail do usuário.");
        }
        email = email.toLowerCase(Locale.ROOT);
        if (email.length() > 120 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new NegocioException("Informe um e-mail de login válido.");
        }
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new NegocioException("Já existe um usuário cadastrado com este e-mail.");
        }
        if (!PasswordPolicy.forte(form.getSenhaInicial())) {
            throw new NegocioException(PasswordPolicy.MENSAGEM);
        }
        // Não permitimos criar outros Admin Master pelo formulário de funcionários.
        String perfil = form.getPerfilUsuario();
        if (!Perfis.GERENTE.equals(perfil) && !Perfis.FUNCIONARIO.equals(perfil)) {
            throw new NegocioException("Selecione um perfil de acesso válido.");
        }
        Funcao funcao = funcoes.findByNome(perfil)
                .orElseThrow(() -> new NegocioException("Perfil não cadastrado no sistema."));
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenhaHash(encoder.encode(form.getSenhaInicial()));
        usuario.setFuncao(funcao);
        return usuario;
    }

    private void aplicar(Funcionario funcionario, FuncionarioForm form, String cpf) {
        funcionario.setNome(form.getNome().trim());
        funcionario.setCpf(cpf);
        funcionario.setTelefone(Textos.limpar(form.getTelefone()));
        funcionario.setEmail(Textos.limpar(form.getEmail()));
        funcionario.setCargo(Textos.limpar(form.getCargo()));
        funcionario.setStatus(form.getStatus());
    }
}
