#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
saida=${1:-target/fccpd}
repeticoes=${BENCH_REPETICOES:-10}
aquecimento=${BENCH_AQUECIMENTO:-3}
java_bin=${JAVA_BIN:-java}
classes=infraestrutura/target/classes
if [[ ! -f "$classes/dev/pedrocaldas/receitopedia/infraestrutura/comunicacao/ProcessadorImagens.class" ]]; then
  echo "Compile primeiro com mvn verify." >&2
  exit 1
fi
mkdir -p "$saida"
{
  date -Iseconds
  uname -a
  "$java_bin" -version 2>&1
  echo "CPUs visíveis ao sistema: $(getconf _NPROCESSORS_ONLN)"
  awk -F ': *' '/model name/{print "CPU: " $2;exit}' /proc/cpuinfo
  for controle in cpu.max memory.max; do
    if [[ -r /sys/fs/cgroup/"$controle" ]]; then
      printf '%s = ' "$controle"
      cat /sys/fs/cgroup/"$controle"
    fi
  done
  docker version --format 'Docker Server {{.Server.Version}}' 2>/dev/null || true
  printf 'Repetições: %s. Aquecimento: %s lotes por combinação.\n' "$repeticoes" "$aquecimento"
  echo "JVM local, -Xmx512m, sem Docker para o processo de medição."
  python -c 'import matplotlib; print("Matplotlib " + matplotlib.__version__)'
} > "$saida/ambiente.txt"
"$java_bin" -Xmx512m -Djava.awt.headless=true --class-path "$classes" scripts/BenchmarkMiniaturas.java "$repeticoes" "$aquecimento" > "$saida/amostras.csv" 2>> "$saida/ambiente.txt"
python scripts/relatorio_benchmark.py "$saida"
