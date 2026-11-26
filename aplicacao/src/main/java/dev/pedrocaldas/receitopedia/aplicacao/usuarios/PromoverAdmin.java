package dev.pedrocaldas.receitopedia.aplicacao.usuarios;

import dev.pedrocaldas.receitopedia.dominio.usuarios.DadosCadastro;

public class PromoverAdmin {
    private final UsuarioRepositorio repositorio;

    public PromoverAdmin(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public boolean executar(String emailConfigurado) {
        String email = DadosCadastro.normalizarEmail(emailConfigurado);
        return !email.isEmpty() && repositorio.promoverAdmin(email);
    }
}
