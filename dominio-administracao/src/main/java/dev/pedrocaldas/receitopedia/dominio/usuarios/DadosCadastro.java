package dev.pedrocaldas.receitopedia.dominio.usuarios;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.EmailValidator;

public record DadosCadastro(String email, String nomeUsuario, String senha) {
    private static final Pattern NOME = Pattern.compile("[a-zA-Z0-9_.-]{3,20}");

    public DadosCadastro {
        var erros = new LinkedHashMap<String, String>();
        email = normalizarEmail(email);
        if (email.length() > 254 || !EmailValidator.getInstance().isValid(email)) {
            erros.put("email", "Informe um e-mail válido.");
        }

        nomeUsuario = nomeUsuario == null ? "" : nomeUsuario.strip();
        if (!NOME.matcher(nomeUsuario).matches()) {
            erros.put("nomeUsuario", "Use 3 a 20 letras, números, pontos, hífens ou sublinhados, sem espaços.");
        }

        if (senha == null || senha.codePoints().count() < 8 || !cabeNoBCrypt(senha)) {
            erros.put("senha", "Use no mínimo 8 caracteres e no máximo 72 bytes em UTF-8.");
        } else if (!temClassesObrigatorias(senha)) {
            erros.put("senha", "Inclua uma letra maiúscula, uma minúscula, um número e um caractere especial.");
        }
        if (!erros.isEmpty()) {
            throw new CadastroInvalido(erros);
        }
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean cabeNoBCrypt(String senha) {
        return senha.length() <= 72 && senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static boolean temClassesObrigatorias(String senha) {
        return senha.codePoints().anyMatch(Character::isUpperCase)
                && senha.codePoints().anyMatch(Character::isLowerCase)
                && senha.codePoints().anyMatch(Character::isDigit)
                && senha.codePoints().anyMatch(c -> !Character.isLetterOrDigit(c)
                        && !Character.isWhitespace(c) && !Character.isISOControl(c));
    }

    @Override
    public String toString() {
        return "DadosCadastro";
    }
}
