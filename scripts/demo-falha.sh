#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
saida=${FCCPD_LOGS:-target/fccpd}
mkdir -p "$saida"
# Salva os logs dos serviços também se a verificação falhar.
trap 'docker compose logs --no-color > "$saida/servicos-falha.log" 2>&1 || true' EXIT
python -u scripts/demo_falha.py "$@" 2>&1 | tee "$saida/demo-falha.log"
