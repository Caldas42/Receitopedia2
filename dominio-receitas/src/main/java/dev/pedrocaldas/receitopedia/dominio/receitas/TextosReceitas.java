package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextosReceitas {
    private static final Pattern ESPACOS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");

    private TextosReceitas() {
    }

    public static String ajustarEspacos(String texto) {
        return texto == null ? "" : ESPACOS.matcher(texto).replaceAll(" ").strip();
    }

    public static String normalizar(String texto) {
        String decomposto = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return ACENTOS.matcher(decomposto).replaceAll("");
    }
}
