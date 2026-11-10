package com.oficina.service;

import com.oficina.exception.NegocioException;
import com.oficina.exception.RecursoNaoEncontradoException;
import com.oficina.form.FuncionarioForm;
import com.oficina.model.Funcao;
import com.oficina.model.Funcionario;
import com.oficina.model.StatusFuncionario;
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
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Um cadastro de funcionário gera automaticamente um usuário, em uma única transação. */
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
    private final SessionRegistry sessoes;

    @Transactional(readOnly = true)
    public List<Funcionario> listar() { return funcionarios.findAllByOrderByNome(); }

    @Transactional(readOnly = true)
    public Funcionario buscar(Long id) {
        return funcionarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado."));
    }

    /** Somente o Admin cria um novo funcionário, porque todo cadastro agora também cria um login. */
    @PreAuthorize("hasRole('ADMIN_MASTER')")
    public Funcionario criar(FuncionarioForm form, UsuarioPrincipal ator) {
        verificarAdmin(ator);
        String cpf = Textos.cpf(form.getCpf());
        if (funcionarios.existsByCpf(cpf)) {
            throw new NegocioException("Já existe um funcionário com este CPF.");
        }
        Usuario usuario = prepararUsuario(form);
        Funcionario funcionario = new Funcionario();
        funcionario.setSalario(BigDecimal.ZERO); // compatibilidade com a coluna NOT NULL existente
        aplicar(funcionario, form, cpf);
        usuario.setAtivo(funcionario.getStatus() == StatusFuncionario.ATIVO);
        funcionario.setUsuario(usuarios.save(usuario));
        funcionario = funcionarios.save(funcionario);
        auditoria.registrar(ator, "CRIAR", "funcionario", funcionario.getId(), "Cadastro de funcionário");
        auditoria.registrar(ator, "CRIAR", "usuario", usuario.getId(),
                "Conta criada automaticamente para funcionário " + funcionario.getId());
        return funcionario;
    }

    /** Atualiza dados funcionais. Para contas antigas sem login, o Admin pode vinculá-lo nesta tela. */
    public Funcionario atualizar(Long id, FuncionarioForm form, UsuarioPrincipal ator) {
        Funcionario funcionario = buscar(id);
        String cpf = Textos.cpf(form.getCpf());
        if (funcionarios.existsByCpfAndIdNot(cpf, id)) {
            throw new NegocioException("Já existe outro funcionário com este CPF.");
        }
        Usuario usuarioNovo = null;
        if (funcionario.getUsuario() == null && (temTexto(form.getSenhaInicial()) || temTexto(form.getPerfilUsuario()))) {
            verificarAdmin(ator);
            usuarioNovo = prepararUsuario(form);
        }
        aplicar(funcionario, form, cpf);
        if (usuarioNovo != null) {
            usuarioNovo.setAtivo(funcionario.getStatus() == StatusFuncionario.ATIVO);
            funcionario.setUsuario(usuarios.save(usuarioNovo));
            auditoria.registrar(ator, "CRIAR", "usuario", usuarioNovo.getId(),
                    "Acesso vinculado a funcionário legado " + id);
        }
        // O acesso acompanha a situação funcional, mas nunca inativa uma conta Admin Master.
        if (funcionario.getUsuario() != null) {
            Usuario vinculado = funcionario.getUsuario();
            boolean deveEstarAtivo = funcionario.getStatus() == StatusFuncionario.ATIVO;
            if (vinculado.isAtivo() != deveEstarAtivo) {
                if (Perfis.ADMIN_MASTER.equals(vinculado.getFuncao().getNome())) {
                    throw new NegocioException("Não é permitido inativar a conta Admin Master pela ficha de funcionário.");
                }
                vinculado.setAtivo(deveEstarAtivo);
                usuarios.save(vinculado);
                if (!deveEstarAtivo) encerrarSessoes(vinculado.getId());
                auditoria.registrar(ator, "ACESSO", "usuario", vinculado.getId(),
                        deveEstarAtivo ? "Acesso reativado junto ao funcionário" : "Acesso inativado junto ao funcionário");
            }
        }
        funcionarios.save(funcionario);
        auditoria.registrar(ator, "EDITAR", "funcionario", id, "Dados do funcionário alterados");
        return funcionario;
    }

    private void encerrarSessoes(Long usuarioId) {
        for (Object principal : sessoes.getAllPrincipals()) {
            if (principal instanceof UsuarioPrincipal p && usuarioId.equals(p.getId())) {
                for (SessionInformation sessao : sessoes.getAllSessions(principal, false)) {
                    sessao.expireNow();
                }
            }
        }
    }

    private void verificarAdmin(UsuarioPrincipal ator) {
        if (ator == null || !ator.isAdmin()) {
            throw new NegocioException("Somente o Admin Master pode criar usuários de acesso.");
        }
    }

    private boolean temTexto(String valor) { return valor != null && !valor.isBlank(); }

    /** O e-mail cadastrado para o funcionário é também seu e-mail de login. */
    private Usuario prepararUsuario(FuncionarioForm form) {
        String email = Textos.limpar(form.getEmail());
        if (email == null || email.isBlank()) throw new NegocioException("Informe o e-mail do funcionário para criar seu login.");
        email = email.toLowerCase(Locale.ROOT);
        if (email.length() > 120 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new NegocioException("Informe um e-mail válido.");
        }
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new NegocioException("Já existe um usuário cadastrado com este e-mail.");
        }
        if (!PasswordPolicy.forte(form.getSenhaInicial())) throw new NegocioException(PasswordPolicy.MENSAGEM);
        String perfil = form.getPerfilUsuario();
        if (!Perfis.GERENTE.equals(perfil) && !Perfis.MECANICO.equals(perfil)
                && !Perfis.ASSISTENTE_ADMINISTRATIVO.equals(perfil)) {
            throw new NegocioException("Selecione um perfil válido: Mecânico, Assistente Administrativo ou Gerente.");
        }
        Funcao funcao = funcoes.findByNome(perfil)
                .orElseThrow(() -> new NegocioException("Perfil não cadastrado no banco."));
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
