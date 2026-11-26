package dev.pedrocaldas.receitopedia.aplicacao.usuarios;

import java.util.function.UnaryOperator;

import dev.pedrocaldas.receitopedia.dominio.usuarios.DadosCadastro;
import dev.pedrocaldas.receitopedia.dominio.usuarios.Usuario;

public class CadastrarUsuario {
    private final UsuarioRepositorio repositorio;
    private final UnaryOperator<String> criptografarSenha;

    public CadastrarUsuario(UsuarioRepositorio repositorio, UnaryOperator<String> criptografarSenha) {
        this.repositorio = repositorio;
        this.criptografarSenha = criptografarSenha;
    }

    public void cadastrar(String email, String nomeUsuario, String senha) {
        var dados = new DadosCadastro(email, nomeUsuario, senha);
        repositorio.adicionar(Usuario.novo(dados.email(), dados.nomeUsuario(), criptografarSenha.apply(dados.senha())));
    }
}
