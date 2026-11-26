import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.imageio.ImageIO;

import dev.pedrocaldas.receitopedia.infraestrutura.comunicacao.ProcessadorImagens;

public class BenchmarkMiniaturas {
    record Cenario(int quantidade, int threads, ProcessadorImagens processador) {}

    public static void main(String[] args) throws Exception {
        int repeticoes = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        if (repeticoes < 10 || warmup < 1) {
            throw new IllegalArgumentException("Use pelo menos 10 repetições e 1 aquecimento.");
        }
        var imagens = new ArrayList<byte[]>();
        var aleatorio = new Random(4708);
        for (int i = 0; i < 4; i++) {
            var imagem = new BufferedImage(1400, 1000, BufferedImage.TYPE_INT_RGB);
            int[] pixels = aleatorio.ints(1400 * 1000, 0, 0x1000000).toArray();
            imagem.setRGB(0, 0, 1400, 1000, pixels, 0, 1400);
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(imagem, i % 2 == 0 ? "jpeg" : "png", bytes);
            imagens.add(bytes.toByteArray());
        }
        System.err.println("Corpus em bytes: " + imagens.stream().map(a -> a.length).toList());
        System.err.println("CPUs disponíveis para a JVM: " + Runtime.getRuntime().availableProcessors());
        var cenarios = new ArrayList<Cenario>();
        try {
            for (int threads : List.of(1, 2, 4)) {
                for (int volume : List.of(1, 2, 4)) {
                    cenarios.add(new Cenario(volume, threads, new ProcessadorImagens(threads)));
                }
            }
            for (int n = 0; n < warmup; n++) {
                Collections.shuffle(cenarios, aleatorio);
                for (var cenario : cenarios) {
                    executar(cenario, imagens);
                }
            }

            System.out.println("imagens,threads,repeticao,milisegundos,imagens_por_segundo");
            for (int i = 1; i <= repeticoes; i++) {
                Collections.shuffle(cenarios, aleatorio);
                for (var cenario : cenarios) {
                    long inicio = System.nanoTime();
                    executar(cenario, imagens);
                    double ms = (System.nanoTime() - inicio) / 1_000_000D;
                    System.out.printf(Locale.ROOT, "%d,%d,%d,%.6f,%.6f%n",
                            cenario.quantidade(), cenario.threads(), i, ms, cenario.quantidade() * 1000D / ms);
                }
            }
        } finally {
            cenarios.forEach(cenario -> cenario.processador().close());
        }
    }

    private static void executar(Cenario cenario, List<byte[]> imagens) {
        var miniaturas = cenario.processador().gerar(imagens.subList(0, cenario.quantidade()));
        if (miniaturas.size() != cenario.quantidade() || miniaturas.stream().anyMatch(m -> m == null || m.length == 0)) {
            throw new IllegalStateException("O processamento não gerou todas as imagens.");
        }
    }
}
