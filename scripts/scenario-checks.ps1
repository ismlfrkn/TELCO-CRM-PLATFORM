# CLAUDE.md Bolum 13'teki 3 kabul senaryosunu (Yeni Abone Onboarding, Aylik Fatura, Kota Asimi)
# api-gateway uzerinden gercek HTTP cagrilariyla adim adim tetikler ve her adimin sonucunu ekrana
# basar. Bu bir otomatik test suite'i DEGIL - assertion/pass-fail yapmaz; kullanicinin "sistem
# gercekten calisiyor mu" diye kendi gozuyle teyit etmesi icin bir runbook'tur (bkz. cdr-simulator.ps1
# ile ayni tarz/yaklasim).
#
# GatewayUrl parametresi sayesinde hem "docker compose up" (varsayilan localhost:8080) hem de
# K8s uzerinde "kubectl port-forward svc/api-gateway 8080:8080 -n telco-crm" sonrasi ayni komutla
# calisir.
#
# On kosul: identity-service'te seed admin kullanicisi (V2__seed_roles_permissions_admin.sql,
# admin/Admin123!) ve tum platform ayakta olmali (bkz. CLAUDE.md Bolum 15 baslatma sirasi ya da
# "kubectl get pods -n telco-crm" ile hepsi Running/Ready).
#
# Kullanim:
#   .\scripts\scenario-checks.ps1
#   .\scripts\scenario-checks.ps1 -GatewayUrl "http://localhost:8080"

param(
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "Admin123!"
)

$ErrorActionPreference = "Stop"

function Write-Step($text) { Write-Host "`n== $text ==" -ForegroundColor Cyan }
function Write-Info($text) { Write-Host "   $text" -ForegroundColor Gray }
function Write-Ok($text) { Write-Host "   OK - $text" -ForegroundColor Green }
function Write-Warn($text) { Write-Host "   UYARI - $text" -ForegroundColor Yellow }
function Write-Fail($text) { Write-Host "   HATA - $text" -ForegroundColor Red }

function Get-AccessToken {
    $body = @{ usernameOrEmail = $Username; password = $Password } | ConvertTo-Json
    $response = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/auth/login" `
        -ContentType "application/json" -Body $body
    return $response.accessToken
}

function Invoke-Checked {
    param(
        [string]$Description,
        [scriptblock]$Action
    )
    try {
        $result = & $Action
        Write-Ok $Description
        return $result
    }
    catch {
        Write-Fail "$Description -> $($_.Exception.Message)"
        throw
    }
}

Write-Step "Giris yapiliyor ($Username)"
$token = Get-AccessToken
$headers = @{ Authorization = "Bearer $token" }
Write-Ok "JWT alindi"

# =============================================================================
# SENARYO 1 - Yeni Abone Onboarding (CLAUDE.md Bolum 14, Senaryo 1)
# =============================================================================
Write-Step "SENARYO 1: Yeni Abone Onboarding"

$identityNumber = "10000000146"  # gecerli TCKN checksum'i (IdentityNumberValidator.isValidTckn)
$customer = Invoke-Checked "Musteri kaydi (POST /customers)" {
    $body = @{
        type           = "INDIVIDUAL"
        firstName      = "Ayse"
        lastName       = "Yilmaz"
        identityNumber = $identityNumber
        dateOfBirth    = "1990-01-01"
        email          = "ayse.yilmaz.$(Get-Date -Format 'HHmmss')@example.com"
        phone          = "+905551112233"
    } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/customers" `
        -Headers $headers -ContentType "application/json" -Body $body
}
$customerId = $customer.id
Write-Info "customerId = $customerId"

Invoke-Checked "KYC belgesi yukleniyor (POST /customers/$customerId/documents)" {
    $body = @{ type = "ID_CARD"; fileRef = "scenario-check-id-card.png" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/customers/$customerId/documents" `
        -Headers $headers -ContentType "application/json" -Body $body
} | Out-Null

Invoke-Checked "KYC onaylaniyor (POST /customers/$customerId/kyc/approve, ADMIN)" {
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/customers/$customerId/kyc/approve" -Headers $headers
} | Out-Null

$tariffCode = "SCENARIO-$(Get-Date -Format 'yyyyMMddHHmmss')"
$dataMbIncluded = 1000
$tariff = Invoke-Checked "Test tarifesi olusturuluyor (POST /tariffs, ADMIN) - code=$tariffCode" {
    $body = @{
        code             = $tariffCode
        name             = "Senaryo Test Tarifesi"
        type             = "POSTPAID"
        monthlyFee       = 100.00
        minutesIncluded  = 500
        smsIncluded      = 250
        dataMbIncluded   = $dataMbIncluded
        status           = "ACTIVE"
        currency         = "TRY"
    } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/tariffs" `
        -Headers $headers -ContentType "application/json" -Body $body
}
Write-Info "tariffCode = $tariffCode, dataMbIncluded = $dataMbIncluded MB"

$order = Invoke-Checked "Postpaid tarife siparisi veriliyor (POST /orders)" {
    $body = @{
        customerId = $customerId
        items      = @(@{ productCode = $tariffCode; productType = "TARIFF"; quantity = 1 })
    } | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/orders" `
        -Headers ($headers + @{ "Idempotency-Key" = [guid]::NewGuid().ToString() }) `
        -ContentType "application/json" -Body $body
}
Write-Info "orderId = $($order.id) (durum: $($order.status))"

Write-Info "Saga (Order -> Payment -> Subscription) Kafka uzerinden asenkron ilerliyor, bekleniyor..."
# 20 x 4s = 80s: Kafka'nin ilk (soguk) consumer group rebalance'i - ozellikle taze bir cluster'da
# topic auto-create + rebalance jenerasyon gecisleri - 30s'lik eski pencereyi asabiliyordu.
$subscription = $null
for ($i = 1; $i -le 20; $i++) {
    Start-Sleep -Seconds 4
    $subs = Invoke-RestMethod -Method Get -Uri "$GatewayUrl/api/v1/subscriptions?customerId=$customerId" -Headers $headers
    $items = if ($subs.content) { $subs.content } else { $subs }
    if ($items -and $items.Count -gt 0) {
        $subscription = $items[0]
        if ($subscription.status -eq "ACTIVE") { break }
    }
    Write-Info "  [$i/20] henuz aktif degil, tekrar deneniyor..."
}

if ($subscription -and $subscription.status -eq "ACTIVE") {
    Write-Ok "Subscription ACTIVE - id=$($subscription.id), msisdn=$($subscription.msisdn)"
}
else {
    Write-Warn "Subscription beklenen surede ACTIVE olmadi - guncel durum: $($subscription | ConvertTo-Json -Compress)"
}

# =============================================================================
# SENARYO 2 - Aylik Fatura (CLAUDE.md Bolum 14, Senaryo 2)
# =============================================================================
Write-Step "SENARYO 2: Aylik Fatura"

$periodStart = (Get-Date -Day 1).ToString("yyyy-MM-dd")
$periodEnd = (Get-Date).ToString("yyyy-MM-dd")
$dueDate = (Get-Date).AddDays(14).ToString("yyyy-MM-dd")

Invoke-Checked "Bill-run tetikleniyor (POST /billing/runs, ADMIN/BILLING_OPERATOR)" {
    $body = @{ periodStart = $periodStart; periodEnd = $periodEnd; dueDate = $dueDate } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/billing/runs" `
        -Headers $headers -ContentType "application/json" -Body $body
} | Out-Null

Write-Info "Fatura + otomatik odeme saga'si Kafka uzerinden asenkron ilerliyor, bekleniyor..."
$invoice = $null
for ($i = 1; $i -le 20; $i++) {
    Start-Sleep -Seconds 4
    $invoices = Invoke-RestMethod -Method Get -Uri "$GatewayUrl/api/v1/invoices?customerId=$customerId" -Headers $headers
    $items = if ($invoices.content) { $invoices.content } else { $invoices }
    if ($items -and $items.Count -gt 0) {
        $invoice = $items[0]
        if ($invoice.status -eq "PAID") { break }
    }
    Write-Info "  [$i/20] fatura henuz PAID degil, tekrar deneniyor..."
}

if ($invoice) {
    Write-Ok "Fatura bulundu - id=$($invoice.id), grandTotal=$($invoice.grandTotal) $($invoice.currency), durum=$($invoice.status)"
}
else {
    Write-Warn "Bu donem icin fatura bulunamadi (bill-run'in aktif abonelik gordugunden emin ol)."
}

# =============================================================================
# SENARYO 3 - Kota Asimi (CLAUDE.md Bolum 14, Senaryo 3)
# =============================================================================
Write-Step "SENARYO 3: Kota Asimi"

if (-not ($subscription -and $subscription.msisdn)) {
    Write-Warn "Senaryo 1'den aktif bir subscription/msisdn alinamadigi icin Senaryo 3 atlaniyor."
}
else {
    function Send-DataCdr {
        param([string]$SubscriptionId, [string]$Msisdn, [double]$Mb)
        $start = (Get-Date).ToUniversalTime()
        $body = @{
            externalCdrId   = [guid]::NewGuid().ToString()
            subscriptionId  = $SubscriptionId
            msisdn          = $Msisdn
            cdrType         = "DATA"
            startTime       = $start.ToString("yyyy-MM-ddTHH:mm:ssZ")
            endTime         = $start.AddSeconds(30).ToString("yyyy-MM-ddTHH:mm:ssZ")
            dataVolumeBytes = [long]($Mb * 1024 * 1024)
            networkType     = "4G"
        } | ConvertTo-Json
        Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/cdr-events" `
            -Headers $headers -ContentType "application/json" -Body $body | Out-Null
    }

    function Show-Quota {
        param([string]$Label)
        $quota = Invoke-RestMethod -Method Get `
            -Uri "$GatewayUrl/api/v1/usage/subscriptions/$($subscription.id)/quota" -Headers $headers
        Write-Info "$Label -> mbRemaining=$($quota.mbRemaining) / $($quota.mbIncluded) MB"
    }

    $eightyPercentMb = [math]::Round($dataMbIncluded * 0.8 / 4)
    Write-Info "Tarifenin %80 esigine ($([math]::Round($dataMbIncluded * 0.8)) MB) ulasmak icin 4 x $eightyPercentMb MB DATA CDR gonderiliyor..."
    for ($i = 1; $i -le 4; $i++) { Send-DataCdr -SubscriptionId $subscription.id -Msisdn $subscription.msisdn -Mb $eightyPercentMb }
    Show-Quota "%80 esigi sonrasi"

    $overageMb = [math]::Round($dataMbIncluded * 0.3)
    Write-Info "%100 esigini asmak icin ek $overageMb MB DATA CDR gonderiliyor..."
    Send-DataCdr -SubscriptionId $subscription.id -Msisdn $subscription.msisdn -Mb $overageMb
    Show-Quota "%100 esigi asildiktan sonra (asim billing'e overage olarak yansimali)"
}

Write-Host "`nTum senaryolar tamamlandi. Yukaridaki OK/UYARI/HATA satirlarini gozden gecirerek" -ForegroundColor Cyan
Write-Host "sistemin beklendigi gibi calisip calismadigini kendin degerlendir." -ForegroundColor Cyan
