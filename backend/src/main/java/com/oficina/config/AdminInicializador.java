package com.oficina.config;

import com.oficina.model.Funcao;
import com.oficina.model.Usuario;
import com.oficina.repository.FuncaoRepository;
import com.oficina.repository.UsuarioRepository;
import com.oficina.security.PasswordPolicy;
import com.oficina.security.Perfis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro administrador na inicialização, só se o banco ainda não tiver nenhum.
 * Não existe senha padrão: o e-mail e a senha vêm do .env, e a senha precisa ser forte.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInicializador implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final FuncaoRepository funcoes;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String senha;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.existsByFuncaoNome(Perfis.ADMIN_MASTER)) {
            log.info("Administrador já existe: nenhum usuário foi criado.");
            return;
        }
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            log.warn("Nenhum administrador no banco. Defina ADMIN_EMAIL e ADMIN_PASSWORD no .env e reinicie.");
            return;
        }
        if (!PasswordPolicy.forte(senha)) {
            log.warn("ADMIN_PASSWORD não atende à política de senha. Administrador não criado. {}",
                    PasswordPolicy.MENSAGEM);
            return;
        }
        Funcao funcao = funcoes.findByNome(Perfis.ADMIN_MASTER)
                .orElseThrow(() -> new IllegalStateException("Perfil ROLE_ADMIN_MASTER não existe: rode o V3."));
        Usuario admin = new Usuario();
        admin.setEmail(email.trim().toLowerCase());
        admin.setSenhaHash(encoder.encode(senha));
        admin.setFuncao(funcao);
        usuarios.save(admin);
        log.info("Administrador criado: {}", admin.getEmail());
    }
}
