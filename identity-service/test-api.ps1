$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:9001/api/v1"

Write-Host "Waiting for service to start on $baseUrl..."
$up = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:9001/actuator/health" -Method Get -ErrorAction SilentlyContinue
        if ($res.StatusCode -eq 200) { $up = $true; break }
    } catch {}
    Start-Sleep -Seconds 1
}
if (-not $up) { Write-Host "Service failed to start."; exit 1 }
Write-Host "Service is up!"

# NOT: identity-service, tum diger servisler gibi X-User-Id / X-User-Roles header'larina guvenir
# (JWT'yi API Gateway dogrular ve bu header'lari enjekte eder). Gateway olmadan dogrudan lokal test
# icin bu header'lari burada elle set ediyoruz - ayrica gateway ile paylasilan internal secret de
# gerekiyor (bkz. configs/application.yaml: gateway.internal-secret), yoksa filtre header'lari yok sayar.
$adminUserId = "99999999-9999-9999-9999-999999999999"
$internalGatewaySecret = "1f4c1c771e1852984d64325c82b2604ad6d20d3b58406c61f1e47b74120a45cd"
$adminHeaders = @{ "X-User-Id" = $adminUserId; "X-User-Roles" = "ADMIN"; "X-Internal-Gateway-Secret" = $internalGatewaySecret }

Write-Host "`n1. Login as seed admin user..."
$loginJson = @"
{
  "usernameOrEmail": "admin",
  "password": "Admin123!"
}
"@
$authResponse = Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/auth/login" -Method Post -Body $loginJson -ContentType "application/json"
$authResponse | ConvertTo-Json -Compress
$accessToken = $authResponse.accessToken
$refreshToken = $authResponse.refreshToken

Write-Host "`n2. GET /api/v1/users/me (gateway header ile)..."
$me = Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/users/me" -Method Get -Headers $adminHeaders
$me | ConvertTo-Json -Depth 4 -Compress

Write-Host "`n3. Creating Role..."
$roleJson = @"
{
  "name": "TEST_ROLE",
  "description": "Test amacli rol"
}
"@
$role = Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/roles" -Method Post -Body $roleJson -ContentType "application/json" -Headers $adminHeaders
$role | ConvertTo-Json -Depth 4 -Compress

Write-Host "`n4. Creating Permission..."
$permissionJson = @"
{
  "code": "TEST_PERMISSION",
  "description": "Test amacli yetki"
}
"@
$permission = Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/permissions" -Method Post -Body $permissionJson -ContentType "application/json" -Headers $adminHeaders
$permission | ConvertTo-Json -Depth 4 -Compress

Write-Host "`n5. Assigning Permission to Role..."
Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/roles/$($role.id)/permissions/$($permission.id)" -Method Post -Headers $adminHeaders | Out-Null

Write-Host "`n6. Registering new User..."
$userJson = @"
{
  "username": "test.agent",
  "email": "test.agent@telco.example",
  "phoneNumber": "+905551112233",
  "password": "TestAgent123!"
}
"@
$newUser = Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/users" -Method Post -Body $userJson -ContentType "application/json" -Headers $adminHeaders
$newUser | ConvertTo-Json -Depth 4 -Compress

Write-Host "`n7. Assigning Role to new User..."
Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/users/$($newUser.id)/roles/$($role.id)" -Method Post -Headers $adminHeaders | Out-Null

Write-Host "`n8. GET /api/v1/users/$($newUser.id) -> Rol atanmis mi?"
$fetchedUser = Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/users/$($newUser.id)" -Method Get -Headers $adminHeaders
$fetchedUser | ConvertTo-Json -Depth 4 -Compress

Write-Host "`n9. Refreshing token (rotation)..."
$refreshJson = "{ ""refreshToken"": ""$refreshToken"" }"
$rotated = Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/auth/refresh" -Method Post -Body $refreshJson -ContentType "application/json"
$rotated | ConvertTo-Json -Compress

Write-Host "`n10. Reusing the OLD refresh token -> reuse detection bekleniyor (401 donmeli)..."
try {
    Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/auth/refresh" -Method Post -Body $refreshJson -ContentType "application/json" | Out-Null
    Write-Host "UYARI: Reuse detection calismadi, eski token kabul edildi!"
} catch {
    Write-Host "Beklendigi gibi reddedildi: $($_.Exception.Response.StatusCode)"
}

Write-Host "`n11. Logout (yeni refresh token ile)..."
$logoutJson = "{ ""refreshToken"": ""$($rotated.refreshToken)"" }"
Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/auth/logout" -Method Post -Body $logoutJson -ContentType "application/json" | Out-Null

Write-Host "`n12. Deleting test User (soft delete)..."
Invoke-RestMethod -UseBasicParsing -Uri "$baseUrl/users/$($newUser.id)" -Method Delete -Headers $adminHeaders | Out-Null

Write-Host "`nAll tests completed!"
