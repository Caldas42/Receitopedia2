package dev.pedrocaldas.receitopedia.apresentacao.usuarios;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import dev.pedrocaldas.receitopedia.aplicacao.usuarios.CadastrarUsuario;
import dev.pedrocaldas.receitopedia.aplicacao.usuarios.UsuarioRepositorio;
import dev.pedrocaldas.receitopedia.dominio.usuarios.DadosCadastro;

import static jakarta.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Configuration
class Seguranca {
    @Bean
    PasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder() {
            @Override
            public boolean matches(CharSequence senha, String hash) {
                return senha != null && DadosCadastro.cabeNoBCrypt(senha.toString()) && super.matches(senha, hash);
            }
        };
    }

    @Bean
    CadastrarUsuario cadastro(UsuarioRepositorio repositorio, PasswordEncoder encoder) {
        return new CadastrarUsuario(repositorio, encoder::encode);
    }

    @Bean
    UserDetailsService credenciais(UsuarioRepositorio repositorio) {
        return email -> repositorio.encontrarPorEmail(DadosCadastro.normalizarEmail(email))
                .map(UsuarioPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http, UserDetailsService credenciais, PasswordEncoder encoder)
            throws Exception {
        var provedor = new DaoAuthenticationProvider(credenciais);
        provedor.setPasswordEncoder(encoder);
        return http
                .authenticationProvider(provedor)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(excecoes -> excecoes.authenticationEntryPoint(
                        (requisicao, resposta, erro) -> resposta.setStatus(SC_UNAUTHORIZED)))
                .authorizeHttpRequests(acesso -> acesso
                        .requestMatchers("/backend/sessao", "/backend/receitas/minhas").authenticated()
                        .requestMatchers(HttpMethod.POST, "/backend/receitas").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/backend/receitas/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/backend/receitas/*").authenticated()
                        .anyRequest().permitAll())
                .formLogin(login -> login
                        .loginProcessingUrl("/backend/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .successHandler((requisicao, resposta, autenticacao) -> resposta.setStatus(SC_NO_CONTENT))
                        .failureHandler((requisicao, resposta, erro) -> {
                            resposta.setStatus(SC_UNAUTHORIZED);
                            resposta.setContentType("application/problem+json;charset=UTF-8");
                            resposta.getWriter().write("""
                                    {"status":401,"detail":"E-mail ou senha inválidos."}
                                    """);
                        }))
                .logout(logout -> logout
                        .logoutUrl("/backend/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((requisicao, resposta, autenticacao) -> resposta.setStatus(SC_NO_CONTENT)))
                .build();
    }
}
