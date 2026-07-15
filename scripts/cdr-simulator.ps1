# PDF Bolum 14.3 (Kota Asimi senaryosu): "CDR simulator usage event'leri uretir" adimini karsilar.
# Repo'da bu adimi otomatik uretecek bir arac yoktu (bkz. CLAUDE.md Bolum 16); bu script
# usage-service'in CdrEventController'ina (POST /api/v1/cdr-events) api-gateway uzerinden N adet
# CDR event'i basar. Once identity-service'ten JWT alir (usage-service dogrudan cagrilirsa
# GatewayHeaderAuthenticationFilter reddeder - CLAUDE.md Bolum 12/13'teki "sadece gateway'e guven"
# modeli geregi).
#
# On kosul: -SubscriptionId, halihazirda SubscriptionActivated olmus GERCEK bir abonelige ait olmali
# (usage-service'te Quota kaydi SubscriptionActivated tuketildiginde acilir - bkz. QuotaService;
# rastgele bir UUID QuotaNotFoundException ile reddedilir). Once Senaryo 1'i (yeni abone onboarding)
# calistirip donen subscription id'sini buraya verin.
#
# Kullanim ornekleri:
#   .\scripts\cdr-simulator.ps1 -SubscriptionId <guid> -Msisdn "+905551234567"
#   .\scripts\cdr-simulator.ps1 -SubscriptionId <guid> -Msisdn "+905551234567" -Count 10 -DataVolumeMb 500
#   .\scripts\cdr-simulator.ps1 -SubscriptionId <guid> -Msisdn "+905551234567" -CdrType VOICE -Count 3

param(
    [Parameter(Mandatory = $true)]
    [string]$SubscriptionId,

    [Parameter(Mandatory = $true)]
    [string]$Msisdn,

    [ValidateSet("DATA", "VOICE", "SMS")]
    [string]$CdrType = "DATA",

    [int]$Count = 5,

    # DATA tipi icin event basina tuketim (MB). Kota asimi tetiklemek istiyorsan tarifenin
    # dataMbIncluded degerine gore Count x DataVolumeMb'yi ona gore ayarla.
    [double]$DataVolumeMb = 200,

    # VOICE tipi icin event basina sure (saniye).
    [int]$DurationSeconds = 300,

    [string]$GatewayUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "Admin123!"
)

$ErrorActionPreference = "Stop"

function Get-AccessToken {
    $body = @{ usernameOrEmail = $Username; password = $Password } | ConvertTo-Json
    try {
        $response = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/auth/login" `
            -ContentType "application/json" -Body $body
        return $response.accessToken
    }
    catch {
        Write-Error "Login basarisiz ($GatewayUrl/api/v1/auth/login): $($_.Exception.Message)"
        throw
    }
}

Write-Host "identity-service'ten JWT aliniyor ($Username)..." -ForegroundColor Cyan
$token = Get-AccessToken
$headers = @{ Authorization = "Bearer $token" }

Write-Host "$Count adet $CdrType CDR event'i uretiliyor -> subscriptionId=$SubscriptionId, msisdn=$Msisdn" -ForegroundColor Cyan

$succeeded = 0
$failed = 0

for ($i = 1; $i -le $Count; $i++) {
    $start = (Get-Date).ToUniversalTime()
    $payload = @{
        externalCdrId = [guid]::NewGuid().ToString()
        subscriptionId = $SubscriptionId
        msisdn = $Msisdn
        cdrType = $CdrType
        startTime = $start.ToString("yyyy-MM-ddTHH:mm:ssZ")
        networkType = "4G"
    }

    switch ($CdrType) {
        "DATA" {
            $payload.dataVolumeBytes = [long]($DataVolumeMb * 1024 * 1024)
            $payload.endTime = $start.AddSeconds(30).ToString("yyyy-MM-ddTHH:mm:ssZ")
        }
        "VOICE" {
            $payload.durationSeconds = $DurationSeconds
            $payload.partyB = "+9055500{0:D4}" -f (Get-Random -Maximum 9999)
            $payload.endTime = $start.AddSeconds($DurationSeconds).ToString("yyyy-MM-ddTHH:mm:ssZ")
        }
        "SMS" {
            $payload.partyB = "+9055500{0:D4}" -f (Get-Random -Maximum 9999)
            $payload.endTime = $start.ToString("yyyy-MM-ddTHH:mm:ssZ")
        }
    }

    try {
        $response = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/v1/cdr-events" `
            -Headers $headers -ContentType "application/json" -Body ($payload | ConvertTo-Json)
        Write-Host "  [$i/$Count] OK - externalCdrId=$($payload.externalCdrId)" -ForegroundColor Green
        $succeeded++
    }
    catch {
        Write-Host "  [$i/$Count] HATA - $($_.Exception.Message)" -ForegroundColor Red
        $failed++
    }
}

Write-Host ""
Write-Host "Tamamlandi: $succeeded basarili, $failed hatali." -ForegroundColor Cyan
if ($CdrType -eq "DATA") {
    $totalMb = $Count * $DataVolumeMb
    Write-Host "Toplam gonderilen data: $totalMb MB - kota esiklerini (%80/%100) asip asmadigini" `
        "GET /api/v1/usage/subscriptions/$SubscriptionId/quota ile kontrol edin." -ForegroundColor Yellow
}
