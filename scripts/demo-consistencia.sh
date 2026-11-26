#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
saida=${FCCPD_LOGS:-target/fccpd}
mkdir -p "$saida"
# Use um banco de testes. CHAT_TEST_DB_URL ausente seleciona o banco H2 em memória.
mvn -pl apresentacao-backend -am test -Dtest=ChatHttpTest,ChatBuscaTest,RegrasChatTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tee "$saida/demo-consistencia.log"
