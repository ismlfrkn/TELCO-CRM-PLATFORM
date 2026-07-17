#!/usr/bin/env bash
# CLAUDE.md Bolum 13'teki 3 kabul senaryosunu (Yeni Abone Onboarding, Aylik Fatura, Kota Asimi)
# api-gateway uzerinden gercek HTTP cagrilariyla adim adim tetikler ve her adimin sonucunu ekrana
# basar. Bkz. scenario-checks.ps1'deki ayni notlar: bu bir assertion/pass-fail suite'i degil,
# kullanicinin kendi gozuyle teyit edecegi bir runbook'tur.
#
# GATEWAY_URL sayesinde hem "docker compose up" (varsayilan localhost:8080) hem de K8s'te
# "kubectl port-forward svc/api-gateway 8080:8080 -n telco-crm" sonrasi ayni komutla calisir.
# Bagimlilik: curl, jq.
#
# Kullanim:
#   ./scripts/scenario-checks.sh
#   GATEWAY_URL="http://localhost:8080" ./scripts/scenario-checks.sh

set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
USERNAME="${USERNAME:-admin}"
PASSWORD="${PASSWORD:-Admin123!}"

command -v jq >/dev/null || { echo "jq gerekli ama bulunamadi." >&2; exit 1; }

step() { echo -e "\n== $1 =="; }
info() { echo "   $1"; }
ok()   { echo "   OK - $1"; }
warn() { echo "   UYARI - $1"; }
fail() { echo "   HATA - $1"; }

api() {
    # api METHOD PATH [BODY_JSON] [EXTRA_HEADER]
    local method="$1" path="$2" body="${3:-}" extra_header="${4:-}"
    local args=(-sf -X "$method" "$GATEWAY_URL$path" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
    [ -n "$extra_header" ] && args+=(-H "$extra_header")
    [ -n "$body" ] && args+=(-d "$body")
    curl "${args[@]}"
}

step "Giris yapiliyor ($USERNAME)"
TOKEN=$(curl -sf -X POST "$GATEWAY_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"usernameOrEmail\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" | jq -r '.accessToken')
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || { fail "Login basarisiz, token alinamadi."; exit 1; }
ok "JWT alindi"

# =============================================================================
# SENARYO 1 - Yeni Abone Onboarding (CLAUDE.md Bolum 14, Senaryo 1)
# =============================================================================
step "SENARYO 1: Yeni Abone Onboarding"

IDENTITY_NUMBER="10000000146"  # gecerli TCKN checksum'i (IdentityNumberValidator.isValidTckn)
EMAIL_SUFFIX=$(date +%H%M%S)

CUSTOMER=$(api POST /api/v1/customers "$(jq -n --arg id "$IDENTITY_NUMBER" --arg email "ayse.yilmaz.$EMAIL_SUFFIX@example.com" '{
    type: "INDIVIDUAL", firstName: "Ayse", lastName: "Yilmaz",
    identityNumber: $id, dateOfBirth: "1990-01-01", email: $email, phone: "+905551112233"
}')") || { fail "Musteri kaydi"; exit 1; }
CUSTOMER_ID=$(echo "$CUSTOMER" | jq -r '.id')
ok "Musteri kaydi (POST /customers)"; info "customerId = $CUSTOMER_ID"

api POST "/api/v1/customers/$CUSTOMER_ID/documents" '{"type":"ID_CARD","fileRef":"scenario-check-id-card.png"}' >/dev/null \
    && ok "KYC belgesi yuklendi" || fail "KYC belgesi yukleme"

api POST "/api/v1/customers/$CUSTOMER_ID/kyc/approve" >/dev/null \
    && ok "KYC onaylandi (ADMIN)" || fail "KYC onay"

TARIFF_CODE="SCENARIO-$(date +%Y%m%d%H%M%S)"
DATA_MB_INCLUDED=1000
api POST /api/v1/tariffs "$(jq -n --arg code "$TARIFF_CODE" --argjson mb "$DATA_MB_INCLUDED" '{
    code: $code, name: "Senaryo Test Tarifesi", type: "POSTPAID", monthlyFee: 100.00,
    minutesIncluded: 500, smsIncluded: 250, dataMbIncluded: $mb, status: "ACTIVE", currency: "TRY"
}')" >/dev/null && ok "Test tarifesi olusturuldu - code=$TARIFF_CODE" || fail "Tarife olusturma"
info "dataMbIncluded = $DATA_MB_INCLUDED MB"

ORDER=$(api POST /api/v1/orders "$(jq -n --arg cid "$CUSTOMER_ID" --arg code "$TARIFF_CODE" '{
    customerId: $cid, items: [{productCode: $code, productType: "TARIFF", quantity: 1}]
}')" "Idempotency-Key: $(cat /proc/sys/kernel/random/uuid 2>/dev/null || python3 -c 'import uuid; print(uuid.uuid4())')") \
    || { fail "Siparis olusturma"; exit 1; }
ORDER_ID=$(echo "$ORDER" | jq -r '.id')
ok "Postpaid tarife siparisi verildi (POST /orders)"; info "orderId = $ORDER_ID"

info "Saga (Order -> Payment -> Subscription) Kafka uzerinden asenkron ilerliyor, bekleniyor..."
# 20 x 4s = 80s: Kafka'nin ilk (soguk) consumer group rebalance'i - ozellikle taze bir cluster'da
# topic auto-create + rebalance jenerasyon gecisleri - 30s'lik eski pencereyi asabiliyordu.
SUBSCRIPTION="{}"
for i in $(seq 1 20); do
    sleep 4
    RESP=$(api GET "/api/v1/subscriptions?customerId=$CUSTOMER_ID")
    SUBSCRIPTION=$(echo "$RESP" | jq -c 'if .content then .content[0] else .[0] end // {}')
    STATUS=$(echo "$SUBSCRIPTION" | jq -r '.status // empty')
    [ "$STATUS" = "ACTIVE" ] && break
    info "  [$i/20] henuz aktif degil, tekrar deneniyor..."
done

MSISDN=$(echo "$SUBSCRIPTION" | jq -r '.msisdn // empty')
SUBSCRIPTION_ID=$(echo "$SUBSCRIPTION" | jq -r '.id // empty')
STATUS=$(echo "$SUBSCRIPTION" | jq -r '.status // empty')
if [ "$STATUS" = "ACTIVE" ]; then
    ok "Subscription ACTIVE - id=$SUBSCRIPTION_ID, msisdn=$MSISDN"
else
    warn "Subscription beklenen surede ACTIVE olmadi - guncel durum: $SUBSCRIPTION"
fi

# =============================================================================
# SENARYO 2 - Aylik Fatura (CLAUDE.md Bolum 14, Senaryo 2)
# =============================================================================
step "SENARYO 2: Aylik Fatura"

PERIOD_START=$(date +%Y-%m-01)
PERIOD_END=$(date +%Y-%m-%d)
DUE_DATE=$(date -u -d "+14 days" +%Y-%m-%d 2>/dev/null || date -u -v+14d +%Y-%m-%d)

api POST /api/v1/billing/runs "$(jq -n --arg s "$PERIOD_START" --arg e "$PERIOD_END" --arg d "$DUE_DATE" \
    '{periodStart: $s, periodEnd: $e, dueDate: $d}')" >/dev/null \
    && ok "Bill-run tetiklendi (ADMIN/BILLING_OPERATOR)" || fail "Bill-run tetikleme"

info "Fatura + otomatik odeme saga'si Kafka uzerinden asenkron ilerliyor, bekleniyor..."
INVOICE="{}"
for i in $(seq 1 20); do
    sleep 4
    RESP=$(api GET "/api/v1/invoices?customerId=$CUSTOMER_ID")
    INVOICE=$(echo "$RESP" | jq -c 'if .content then .content[0] else .[0] end // {}')
    STATUS=$(echo "$INVOICE" | jq -r '.status // empty')
    [ "$STATUS" = "PAID" ] && break
    info "  [$i/20] fatura henuz PAID degil, tekrar deneniyor..."
done

INVOICE_ID=$(echo "$INVOICE" | jq -r '.id // empty')
if [ -n "$INVOICE_ID" ]; then
    ok "Fatura bulundu - id=$INVOICE_ID, grandTotal=$(echo "$INVOICE" | jq -r '.grandTotal') $(echo "$INVOICE" | jq -r '.currency'), durum=$(echo "$INVOICE" | jq -r '.status')"
else
    warn "Bu donem icin fatura bulunamadi (bill-run'in aktif abonelik gordugunden emin ol)."
fi

# =============================================================================
# SENARYO 3 - Kota Asimi (CLAUDE.md Bolum 14, Senaryo 3)
# =============================================================================
step "SENARYO 3: Kota Asimi"

if [ "$STATUS" != "ACTIVE" ] || [ -z "$MSISDN" ]; then
    warn "Senaryo 1'den aktif bir subscription/msisdn alinamadigi icin Senaryo 3 atlaniyor."
else
    send_data_cdr() {
        local mb="$1"
        local start end
        start=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
        end=$(date -u -d "+30 seconds" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -v+30S +"%Y-%m-%dT%H:%M:%SZ")
        local vol
        vol=$(awk -v mb="$mb" 'BEGIN{printf "%d", mb*1024*1024}')
        local eid
        eid=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || python3 -c 'import uuid; print(uuid.uuid4())')
        api POST /api/v1/cdr-events "$(jq -n --arg eid "$eid" --arg sid "$SUBSCRIPTION_ID" --arg msisdn "$MSISDN" \
            --arg start "$start" --arg end "$end" --argjson vol "$vol" \
            '{externalCdrId:$eid, subscriptionId:$sid, msisdn:$msisdn, cdrType:"DATA", startTime:$start, endTime:$end, dataVolumeBytes:$vol, networkType:"4G"}')" >/dev/null
    }

    show_quota() {
        local label="$1"
        local q
        q=$(api GET "/api/v1/usage/subscriptions/$SUBSCRIPTION_ID/quota")
        info "$label -> mbRemaining=$(echo "$q" | jq -r '.mbRemaining') / $(echo "$q" | jq -r '.mbIncluded') MB"
    }

    eighty_pct_mb=$(awk -v mb="$DATA_MB_INCLUDED" 'BEGIN{printf "%d", (mb*0.8)/4}')
    info "Tarifenin %80 esigine ulasmak icin 4 x ${eighty_pct_mb} MB DATA CDR gonderiliyor..."
    for i in 1 2 3 4; do send_data_cdr "$eighty_pct_mb"; done
    show_quota "%80 esigi sonrasi"

    overage_mb=$(awk -v mb="$DATA_MB_INCLUDED" 'BEGIN{printf "%d", mb*0.3}')
    info "%100 esigini asmak icin ek ${overage_mb} MB DATA CDR gonderiliyor..."
    send_data_cdr "$overage_mb"
    show_quota "%100 esigi asildiktan sonra (asim billing'e overage olarak yansimali)"
fi

echo ""
echo "Tum senaryolar tamamlandi. Yukaridaki OK/UYARI/HATA satirlarini gozden gecirerek"
echo "sistemin beklendigi gibi calisip calismadigini kendin degerlendir."
