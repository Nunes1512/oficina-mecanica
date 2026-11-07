package com.oficina.config;

import com.oficina.security.LoginAttemptService;
import com.oficina.security.LoginBloqueioFilter;
import com.oficina.security.LoginFalhaHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * Regras de segurança da aplicação:
 * - login por formulário com sessão (cookie HttpOnly, SameSite, id trocado após o login);
 * - bloqueio temporário após várias senhas erradas, com aviso na tela de login;
 * - proteção CSRF ligada (padrão do Spring Security; os formulários Thymeleaf enviam o token);
 * - acesso por perfil nas rotas e, de novo, nos métodos (@PreAuthorize);
 * - cabeçalhos HTTP de segurança, incluindo uma política de conteúdo (CSP) sem scripts inline.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginAttemptService tentativas) throws Exception {
        http.addFilterBefore(new LoginBloqueioFilter(tentativas), UsernamePasswordAuthenticationFilter.class);
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico", "/login", "/error").permitAll()
                .requestMatchers("/usuarios/**", "/auditoria/**").hasRole("ADMIN_MASTER")
                .requestMatchers("/funcionarios/**", "/clientes/**", "/pecas/**", "/dashboard/**")
                    .hasAnyRole("ADMIN_MASTER", "GERENTE")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureHandler(new LoginFalhaHandler(tentativas))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?saiu")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId())
                .invalidSessionUrl("/login?expirou"))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                        "camera=(), microphone=(), geolocation=()")));
        return http.build();
    }

    /** BCrypt com custo 12. Os hashes antigos (custo 10, do V8) continuam válidos. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
