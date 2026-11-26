import csv
import math
from collections import defaultdict
from pathlib import Path
import statistics
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt


saida = Path(sys.argv[1])
resultados = defaultdict(list)
with (saida / "amostras.csv").open() as arquivo:
    for linha in csv.DictReader(arquivo):
        chave = (int(linha["imagens"]), int(linha["threads"]))
        resultados[chave].append(float(linha["milisegundos"]))

linhas = []
for (volume, threads), dados in sorted(resultados.items()):
    valores = sorted(dados)
    media = statistics.mean(valores)
    linhas.append({
        "imagens": volume, "threads": threads, "media_ms": media,
        "mediana_ms": statistics.median(valores),
        "p95_ms": valores[math.ceil(.95 * len(valores)) - 1],
        "desvio_ms": statistics.stdev(valores),
        "vazao_imagens_s": volume * 1000 / media,
        "ganho": statistics.mean(resultados[volume, 1]) / media,
    })

with (saida / "resumo.csv").open("w", newline="") as arquivo:
    escritor = csv.DictWriter(arquivo, fieldnames=list(linhas[0]))
    escritor.writeheader()
    escritor.writerows({
        chave: f"{valor:.3f}" if isinstance(valor, float) else valor
        for chave, valor in linha.items()
    } for linha in linhas)

figura, eixo = plt.subplots(figsize=(7.6, 4.6), layout="constrained")
for threads, cor in [(1, "#72504a"), (2, "#126d65"), (4, "#596aab")]:
    dados = [linha for linha in linhas if linha["threads"] == threads]
    eixo.errorbar([r["imagens"] for r in dados], [r["media_ms"] for r in dados],
                  yerr=[r["desvio_ms"] for r in dados], marker="o", capsize=4, color=cor,
                  label=f"{threads} thread" + ("s" if threads > 1 else " (sequencial)"))
eixo.set(title="Geração de miniaturas JPEG por lote", xlabel="Imagens por lote",
         ylabel="Tempo médio por lote (ms)")
eixo.set_xticks(sorted({linha["imagens"] for linha in linhas}))
eixo.grid(axis="y", alpha=.25)
eixo.legend()
figura.savefig(saida / "miniaturas.png", dpi=180)
