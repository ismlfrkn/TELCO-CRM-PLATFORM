$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:9003/api/v1"

# Helper to print and swallow error for expected failures
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
        $errResp | ConvertTo-Json -Depth 2 -Compress
    }
}

Write-Host "--- 1. POST Tariff (TRF-002) ---"
$tariff = @{ code="TRF-002"; name="Super Tariff"; type="POSTPAID"; monthlyFee=150.0; minutesIncluded=1000; smsIncluded=1000; dataMbIncluded=10240; status="ACTIVE"; effectiveFrom="2026-07-01" } | ConvertTo-Json
Invoke-SafeRestMethod -Uri "$baseUrl/tariffs" -Method Post -Body $tariff

Write-Host "`n--- 2. POST ProductOffering (Valid Tariff) ---"
$po1 = @{ code="PO-002"; name="Premium Offering"; description="Premium Pack"; tariffCode="TRF-002"; status="ACTIVE"; effectiveFrom="2026-07-01" } | ConvertTo-Json
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings" -Method Post -Body $po1

Write-Host "`n--- 3. POST ProductOffering (Invalid Tariff TRF-999) ---"
$po2 = @{ code="PO-002"; name="Bad Offering"; description="Bad Pack"; tariffCode="TRF-999"; status="ACTIVE"; effectiveFrom="2026-07-01" } | ConvertTo-Json
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings" -Method Post -Body $po2

Write-Host "`n--- 4. GET ProductOfferings (Pagination) ---"
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings?page=0&size=5" -Method Get | ConvertTo-Json -Depth 4 -Compress

Write-Host "`n--- 5. PATCH ProductOffering ---"
$poPatch = @{ description="Updated Pack" } | ConvertTo-Json
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings/PO-002" -Method Patch -Body $poPatch

Write-Host "`n--- 6. GET ProductOfferings (After PATCH) ---"
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings?page=0&size=5" -Method Get | ConvertTo-Json -Depth 4 -Compress

Write-Host "`n--- 7. DELETE ProductOffering ---"
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings/PO-002" -Method Delete

Write-Host "`n--- 8. GET ProductOfferings (After DELETE) ---"
Invoke-SafeRestMethod -Uri "$baseUrl/product-offerings?page=0&size=5" -Method Get | ConvertTo-Json -Depth 4 -Compress

