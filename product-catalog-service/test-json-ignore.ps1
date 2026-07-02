$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:9003/api/v1"

function Invoke-SafeRestMethod {
    param($Uri, $Method, $Body)
    try {
        if ($Body) {
            Invoke-RestMethod -Uri $Uri -Method $Method -Body $Body -ContentType "application/json"
        } else {
            Invoke-RestMethod -Uri $Uri -Method $Method
        }
    } catch {
        $streamReader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $errResp = $streamReader.ReadToEnd() | ConvertFrom-Json
        $errResp | ConvertTo-Json -Depth 5 -Compress
    }
}

Write-Host "--- 1. POST Tariff (TRF-003) ---"
$tariff = @{ code="TRF-003"; name="Tariff 3"; type="POSTPAID"; monthlyFee=100.0; minutesIncluded=500; smsIncluded=500; dataMbIncluded=5120; status="ACTIVE"; effectiveFrom="2026-07-01" } | ConvertTo-Json
Invoke-SafeRestMethod -Uri "$baseUrl/tariffs" -Method Post -Body $tariff

Write-Host "`n--- 2. POST Addon (ADN-003) ---"
$addon = @{ code="ADN-003"; name="Addon 3"; price=10.0; type="DATA"; validityDays=30; status="ACTIVE" } | ConvertTo-Json
Invoke-SafeRestMethod -Uri "$baseUrl/addons" -Method Post -Body $addon

Write-Host "`n--- 3. POST ProductOffering (PO-003) ---"
$po = @{ code="PO-003"; name="Offering 3"; description="Offering 3"; tariffCode="TRF-003"; status="ACTIVE"; effectiveFrom="2026-07-01" } | ConvertTo-Json
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings" -Method Post -Body $po

Write-Host "`n--- 4. Link Addon to Tariff ---"
Invoke-SafeRestMethod -Uri "$baseUrl/tariffs/TRF-003/addons/ADN-003" -Method Post

Write-Host "`n--- 5. GET ProductOfferings (JSON serialization test) ---"
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings/PO-003" -Method Get | ConvertTo-Json -Depth 6

