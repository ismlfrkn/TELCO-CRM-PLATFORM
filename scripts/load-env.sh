#!/usr/bin/env bash
# .env dosyasini okuyup mevcut shell oturumuna ortam degiskeni olarak yukler.
# Source ile calistirilmali ki degiskenler bu oturumda kalsin:
#   source scripts/load-env.sh

ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "Uyari: .env bulunamadi ($ENV_FILE). Once 'cp .env.example .env' ile olusturun." >&2
    return 1 2>/dev/null || exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

echo ".env yuklendi: $ENV_FILE"
