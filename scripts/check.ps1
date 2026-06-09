$ErrorActionPreference = "Stop"

function Test-Endpoint {
    param(
        [string] $Name,
        [string] $Url
    )

    try {
        $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 5
        Write-Host "[OK] $Name $Url"
        $response | ConvertTo-Json -Depth 6
    } catch {
        Write-Host "[FAIL] $Name $Url"
        Write-Host $_.Exception.Message
    }
}

Write-Host "== Docker Compose =="
docker compose ps

Write-Host ""
Write-Host "== Health Checks =="
Test-Endpoint -Name "Backend" -Url "http://localhost:8080/api/health"
Test-Endpoint -Name "CV" -Url "http://localhost:8000/cv/health"

