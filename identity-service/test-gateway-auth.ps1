$ErrorActionPreference = "Stop"

$gatewayUrl = "http://localhost:8080/api/v1"
$identityDirectUrl = "http://localhost:9001/api/v1"

function Wait-ForHealth($healthUrl, $label) {
    Write-Host "Waiting for $label on $healthUrl..."
    for ($i = 0; $i -lt 30; $i++) {
        try {
            $res = Invoke-WebRequest -Uri $healthUrl -Method Get -UseBasicParsing -ErrorAction SilentlyContinue
            if ($res.StatusCode -eq 200) { Write-Host "$label is up!"; return }
        } catch {}
        Start-Sleep -Seconds 1
    }
    Write-Host "$label failed to start."; exit 1
}

Wait-ForHealth "http://localhost:9001/actuator/health" "identity-service"
Wait-ForHealth "http://localhost:8080/actuator/health" "api-gateway"

# --- Senaryo (a): token'siz istek, gateway uzerinden -> 401 bekleniyor ---
Write-Host "`n(a) Token'siz GET $gatewayUrl/users -> 401 bekleniyor..."
try {
    Invoke-RestMethod -UseBasicParsing -Uri "$gatewayUrl/users" -Method Get | Out-Null
    Write-Host "UYARI: 401 bekleniyordu ama istek basarili oldu!"
} catch {
    $status = $_.Exception.Response.StatusCode
    if ($status -eq 401) { Write-Host "OK: 401 alindi." } else { Write-Host "UYARI: Beklenmeyen durum kodu: $status" }
}

# --- Senaryo (b): gecerli admin token'i ile gateway uzerinden -> 200 bekleniyor ---
Write-Host "`n(b) Login (gateway uzerinden, /api/v1/auth/** muaf rotadir)..."
$loginJson = @"
{
  "usernameOrEmail": "admin",
  "password": "Admin123!"
}
"@
$authResponse = Invoke-RestMethod -UseBasicParsing -Uri "$gatewayUrl/auth/login" -Method Post -Body $loginJson -ContentType "application/json"
$accessToken = $authResponse.accessToken
Write-Host "Access token alindi."

Write-Host "GET $gatewayUrl/users (Authorization: Bearer <token>) -> 200 bekleniyor..."
$authHeaders = @{ "Authorization" = "Bearer $accessToken" }
$users = Invoke-RestMethod -UseBasicParsing -Uri "$gatewayUrl/users" -Method Get -Headers $authHeaders
Write-Host "OK: $($users.content.Count) kullanici donduruldu."

# --- Senaryo (c): identity-service'e dogrudan (gateway bypass), sahte X-User-Id/X-User-Roles ile -> 401 bekleniyor ---
Write-Host "`n(c) identity-service'e (9001) dogrudan, internal secret OLMADAN sahte header ile -> 401 bekleniyor..."
$forgedHeaders = @{ "X-User-Id" = "99999999-9999-9999-9999-999999999999"; "X-User-Roles" = "ADMIN" }
try {
    Invoke-RestMethod -UseBasicParsing -Uri "$identityDirectUrl/users" -Method Get -Headers $forgedHeaders | Out-Null
    Write-Host "UYARI: internal secret olmadan istek kabul edildi - GUVENLIK ACIGI!"
} catch {
    $status = $_.Exception.Response.StatusCode
    if ($status -eq 401) { Write-Host "OK: internal secret olmadan sahte header reddedildi (401)." } else { Write-Host "UYARI: Beklenmeyen durum kodu: $status" }
}

Write-Host "`nAll gateway auth tests completed!"
