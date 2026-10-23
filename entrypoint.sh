#!/bin/sh
set -eu

ENV_FILE="/app/.env"

if [ -f "$ENV_FILE" ]; then
    set -a
    . "$ENV_FILE"
    set +a
else
    echo "Utilizando configuracoes do ambiente."
fi

echo "Iniciando MeuFinanceiro API..."

exec java -jar /app/app.jar