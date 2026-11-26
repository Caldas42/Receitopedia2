package dev.pedrocaldas.receitopedia.infraestrutura.comunicacao;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class ProcessadorImagens implements AutoCloseable {
    private final ExecutorService pool;

    public ProcessadorImagens(int threads) {
        if (threads < 1 || threads > 8) {
            throw new IllegalArgumentException("Configure entre 1 e 8 threads de imagens.");
        }
        pool = threads == 1 ? null : Executors.newFixedThreadPool(threads);
    }

    public List<byte[]> gerar(List<byte[]> originais) {
        if (pool == null || originais.size() <= 1) {
            return originais.stream().map(ProcessadorImagens::redimensionar).toList();
        }
        List<Callable<byte[]>> tarefas = originais.stream()
                .<Callable<byte[]>>map(original -> () -> redimensionar(original)).toList();
        try {
            var resultados = new ArrayList<byte[]>(originais.size());
            for (var resultado : pool.invokeAll(tarefas)) {
                resultados.add(resultado.get());
            }
            return resultados;
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Geração de miniaturas interrompida.", erro);
        } catch (ExecutionException erro) {
            throw new IllegalStateException("Falha ao gerar miniaturas.", erro.getCause());
        }
    }

    private static byte[] redimensionar(byte[] bytes) {
        try (var entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                return null;
            }
            var leitor = leitores.next();
            BufferedImage imagem;
            try {
                leitor.setInput(entrada, true, true);
                if ((long) leitor.getWidth(0) * leitor.getHeight(0) > 12_000_000) {
                    return null;
                }
                imagem = leitor.read(0);
            } finally {
                leitor.dispose();
            }
            double escala = Math.min(1D, 480D / Math.max(imagem.getWidth(), imagem.getHeight()));
            var miniatura = new BufferedImage(Math.max(1, (int) (imagem.getWidth() * escala)),
                    Math.max(1, (int) (imagem.getHeight() * escala)), BufferedImage.TYPE_INT_RGB);
            var desenho = miniatura.createGraphics();
            try {
                desenho.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                desenho.drawImage(imagem, 0, 0, miniatura.getWidth(), miniatura.getHeight(), Color.WHITE, null);
            } finally {
                desenho.dispose();
            }
            var bytesMiniatura = new ByteArrayOutputStream();
            try (var saida = new MemoryCacheImageOutputStream(bytesMiniatura)) {
                ImageIO.write(miniatura, "jpeg", saida);
            }
            return bytesMiniatura.toByteArray();
        } catch (IOException erro) {
            // A mensagem pode ser entregue com o original mesmo sem uma miniatura decodificável.
            return null;
        }
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }
}
