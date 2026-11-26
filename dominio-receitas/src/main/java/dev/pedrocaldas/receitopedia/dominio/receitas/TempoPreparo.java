package dev.pedrocaldas.receitopedia.dominio.receitas;

public record TempoPreparo(int minutosTotais) {
    private static final int MAX_HORAS = 99;

    public TempoPreparo {
        if (minutosTotais < 0 || minutosTotais > MAX_HORAS * 60 + 59) {
            throw new IllegalArgumentException("Informe um tempo de preparo entre 0 e 99h 59min.");
        }
    }

    public static TempoPreparo informado(Integer horas, Integer minutos) {
        if (horas == null && minutos == null) {
            return null;
        }
        int totalHoras = horas == null ? 0 : horas;
        int parteMinutos = minutos == null ? 0 : minutos;
        if (totalHoras < 0 || totalHoras > MAX_HORAS) {
            throw new IllegalArgumentException("Informe horas inteiras entre 0 e 99.");
        }
        if (parteMinutos < 0 || parteMinutos > 59) {
            throw new IllegalArgumentException("Informe minutos inteiros entre 0 e 59.");
        }
        return new TempoPreparo(totalHoras * 60 + parteMinutos);
    }
}
