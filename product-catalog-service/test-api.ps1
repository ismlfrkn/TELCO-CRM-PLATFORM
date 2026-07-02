$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:9003/api/v1"

Write-Host "Waiting for service to start on $baseUrl..."
$up = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $res = Invoke-WebRequest -Uri "$baseUrl/tariffs" -Method Get -ErrorAction SilentlyContinue
        if ($res.StatusCode -eq 200) { $up = $true; break }
    } catch {}
    Start-Sleep -Seconds 1
}
if (-not $up) { Write-Host "Service failed to start."; exit 1 }
Write-Host "Service is up!"

$tariffJson = @"
{
  "code": "TRF-001",
  "name": "Super Tariff",
  "type": "POSTPAID",
  "monthlyFee": 150.00,
  "minutesIncluded": 1000,
  "smsIncluded": 1000,
  "dataMbIncluded": 10240,
  "status": "ACTIVE",
  "effectiveFrom": "2026-07-01"
}
"@
Write-Host "`n1. Creating Tariff..."
Invoke-RestMethod -Uri "$baseUrl/tariffs" -Method Post -Body $tariffJson -ContentType "application/json" | Out-Null

$addonJson = @"
{
  "code": "ADN-001",
  "name": "Extra 5GB",
  "price": 50.00,
  "type": "DATA",
  "validityDays": 30
}
"@
Write-Host "2. Creating Addon..."
Invoke-RestMethod -Uri "$baseUrl/addons" -Method Post -Body $addonJson -ContentType "application/json" | Out-Null

Write-Host "3. Linking Addon to Tariff..."
Invoke-RestMethod -Uri "$baseUrl/tariffs/TRF-001/addons/ADN-001" -Method Post | Out-Null

Write-Host "`n4. GET /api/v1/addons (filtresiz) -> Sonuç geliyor mu?"
$allAddons = Invoke-RestMethod -Uri "$baseUrl/addons" -Method Get
$allAddons.content | ConvertTo-Json -Depth 2 -Compress

Write-Host "`n5. GET /api/v1/addons?tariffCode=TRF-001 -> Dogru filtrelenmis sonuc geliyor mu?"
$filteredAddons = Invoke-RestMethod -Uri "$baseUrl/addons?tariffCode=TRF-001" -Method Get
$filteredAddons.content | ConvertTo-Json -Depth 2 -Compress

Write-Host "`n6. Deleting Tariff (TRF-001)..."
Invoke-RestMethod -Uri "$baseUrl/tariffs/TRF-001" -Method Delete | Out-Null

Write-Host "`n7. GET /api/v1/tariffs -> Silinen kayit listede gorunmemeli!"
$tariffsAfterDelete = Invoke-RestMethod -Uri "$baseUrl/tariffs" -Method Get
$tariffsAfterDelete.content | ConvertTo-Json -Depth 2 -Compress

Write-Host "`nAll tests completed!"
