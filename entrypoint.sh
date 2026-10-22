#!/bin/sh
set -eu

ENV_FILE="/app/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "Erro: arquivo .env não encontrado."
    exit 1
fi

echo "Carregando variáveis de ambiente..."

set -a
. "$ENV_FILE"
set +a

echo "Iniciando MeuFinanceiro API..."

exec java -jar /app/app.jar