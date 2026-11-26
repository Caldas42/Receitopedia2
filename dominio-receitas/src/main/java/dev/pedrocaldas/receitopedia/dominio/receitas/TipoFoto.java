package dev.pedrocaldas.receitopedia.dominio.receitas;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public enum TipoFoto {
    JPEG("image/jpeg"), PNG("image/png"), WEBP("image/webp");

    public static final int LIMITE_BYTES = 5 * 1024 * 1024;
    private static final int[] ASSINATURA_PNG = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private final String mime;

    TipoFoto(String mime) {
        this.mime = mime;
    }

    public String mime() {
        return mime;
    }

    public static TipoFoto detectar(byte[] bytes) {
        if (bytes.length > LIMITE_BYTES) {
            throw new IllegalArgumentException("Cada foto deve ter no máximo 5 MB.");
        }
        if (bytes.length >= 12 && coincide(bytes, 0, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        if (bytes.length >= 33 && coincide(bytes, 0, ASSINATURA_PNG)
                && coincide(bytes, 8, 0, 0, 0, 13, 'I', 'H', 'D', 'R')) {
            return PNG;
        }
        if (bytes.length >= 20 && coincide(bytes, 0, 'R', 'I', 'F', 'F')
                && coincide(bytes, 8, 'W', 'E', 'B', 'P', 'V', 'P', '8')
                && (bytes[15] == ' ' || bytes[15] == 'L' || bytes[15] == 'X')
                && Integer.toUnsignedLong(ByteBuffer.wrap(bytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt())
                        == bytes.length - 8) {
            return WEBP;
        }
        throw new IllegalArgumentException("Envie uma imagem JPEG, PNG ou WebP válida.");
    }

    private static boolean coincide(byte[] bytes, int inicio, int... assinatura) {
        for (int i = 0; i < assinatura.length; i++) {
            if (Byte.toUnsignedInt(bytes[inicio + i]) != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}
