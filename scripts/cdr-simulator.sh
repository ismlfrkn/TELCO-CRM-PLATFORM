#!/usr/bin/env bash
# PDF Bolum 14.3 (Kota Asimi senaryosu): "CDR simulator usage event'leri uretir" adimini karsilar.
# Bkz. cdr-simulator.ps1'deki ayni notlar (on kosul: SUBSCRIPTION_ID gercekten aktif olmus bir
# abonelige ait olmali, aksi halde usage-service QuotaNotFoundException doner).
# Bagimlilik: curl, jq.
#
# Kullanim:
#   SUBSCRIPTION_ID=<guid> MSISDN="+905551234567" ./scripts/cdr-simulator.sh
#   SUBSCRIPTION_ID=<guid> MSISDN="+905551234567" COUNT=10 DATA_VOLUME_MB=500 ./scripts/cdr-simulator.sh
#   SUBSCRIPTION_ID=<guid> MSISDN="+905551234567" CDR_TYPE=VOICE COUNT=3 ./scripts/cdr-simulator.sh

set -euo pipefail

: "${SUBSCRIPTION_ID:?SUBSCRIPTION_ID env degiskeni gerekli - aktif bir aboneligin id degeri}"
: "${MSISDN:?MSISDN env degiskeni gerekli}"

CDR_TYPE="${CDR_TYPE:-DATA}"
COUNT="${COUNT:-5}"
DATA_VOLUME_MB="${DATA_VOLUME_MB:-200}"
DURATION_SECONDS="${DURATION_SECONDS:-300}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-Admin123!}"

command -v jq >/dev/null || { echo "jq gerekli ama bulunamadi." >&2; exit 1; }

echo "identity-service'ten JWT aliniyor ($USERNAME)..."
TOKEN=$(curl -sf -X POST "$GATEWAY_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"usernameOrEmail\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" | jq -r '.accessToken')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "Login basarisiz, token alinamadi." >&2
    exit 1
fi

echo "$COUNT adet $CDR_TYPE CDR event'i uretiliyor -> subscriptionId=$SUBSCRIPTION_ID, msisdn=$MSISDN"

succeeded=0
failed=0

for i in $(seq 1 "$COUNT"); do
    external_id=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || python3 -c 'import uuid; print(uuid.uuid4())')
    start_time=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    case "$CDR_TYPE" in
        DATA)
            data_volume_bytes=$(awk -v mb="$DATA_VOLUME_MB" 'BEGIN{printf "%d", mb*1024*1024}')
            end_time=$(date -u -d "+30 seconds" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -v+30S +"%Y-%m-%dT%H:%M:%SZ")
            payload=$(jq -n --arg eid "$external_id" --arg sid "$SUBSCRIPTION_ID" --arg msisdn "$MSISDN" \
                --arg start "$start_time" --arg end "$end_time" --argjson vol "$data_volume_bytes" \
                '{externalCdrId:$eid, subscriptionId:$sid, msisdn:$msisdn, cdrType:"DATA", startTime:$start, endTime:$end, dataVolumeBytes:$vol, networkType:"4G"}')
            ;;
        VOICE)
            end_time=$(date -u -d "+$DURATION_SECONDS seconds" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -v+"${DURATION_SECONDS}"S +"%Y-%m-%dT%H:%M:%SZ")
            party_b="+90555$((RANDOM % 9000 + 1000))"
            payload=$(jq -n --arg eid "$external_id" --arg sid "$SUBSCRIPTION_ID" --arg msisdn "$MSISDN" \
                --arg start "$start_time" --arg end "$end_time" --argjson dur "$DURATION_SECONDS" --arg partyb "$party_b" \
                '{externalCdrId:$eid, subscriptionId:$sid, msisdn:$msisdn, cdrType:"VOICE", startTime:$start, endTime:$end, durationSeconds:$dur, partyB:$partyb, networkType:"4G"}')
            ;;
        SMS)
            party_b="+90555$((RANDOM % 9000 + 1000))"
            payload=$(jq -n --arg eid "$external_id" --arg sid "$SUBSCRIPTION_ID" --arg msisdn "$MSISDN" \
                --arg start "$start_time" --arg partyb "$party_b" \
                '{externalCdrId:$eid, subscriptionId:$sid, msisdn:$msisdn, cdrType:"SMS", startTime:$start, endTime:$start, partyB:$partyb, networkType:"4G"}')
            ;;
        *)
            echo "Desteklenmeyen CDR_TYPE: $CDR_TYPE (DATA|VOICE|SMS)" >&2
            exit 1
            ;;
    esac

    if curl -sf -X POST "$GATEWAY_URL/api/v1/cdr-events" \
        -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
        -d "$payload" >/dev/null; then
        echo "  [$i/$COUNT] OK - externalCdrId=$external_id"
        succeeded=$((succeeded + 1))
    else
        echo "  [$i/$COUNT] HATA"
        failed=$((failed + 1))
    fi
done

echo ""
echo "Tamamlandi: $succeeded basarili, $failed hatali."
if [ "$CDR_TYPE" = "DATA" ]; then
    total_mb=$(awk -v c="$COUNT" -v mb="$DATA_VOLUME_MB" 'BEGIN{printf "%d", c*mb}')
    echo "Toplam gonderilen data: ${total_mb} MB - kota esiklerini (%80/%100) asip asmadigini" \
        "GET /api/v1/usage/subscriptions/$SUBSCRIPTION_ID/quota ile kontrol edin."
fi
