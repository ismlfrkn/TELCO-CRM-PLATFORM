# .env dosyasini okuyup mevcut PowerShell oturumuna ortam degiskeni olarak yukler.
# Dot-source ile calistirilmali ki degiskenler bu oturumda kalsin:
#   . .\scripts\load-env.ps1
# (Normal calistirma - .\scripts\load-env.ps1 - degiskenleri alt process'te ayarlar ve
# script bitince kaybolur.)

$envFile = Join-Path $PSScriptRoot "..\.env"

if (-not (Test-Path $envFile)) {
    Write-Warning ".env bulunamadi ($envFile). Once 'Copy-Item .env.example .env' ile olusturun."
    return
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) {
        return
    }
    $parts = $line -split "=", 2
    if ($parts.Length -eq 2) {
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        Set-Item -Path "env:$name" -Value $value
    }
}

Write-Host ".env yuklendi: $envFile" -ForegroundColor Green
