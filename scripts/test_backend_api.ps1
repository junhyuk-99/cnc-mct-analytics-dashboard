param(
    [string]$BaseUrl = "http://localhost:8090/api"
)

$ErrorActionPreference = "Stop"

$endpoints = @(
    "/machines",
    "/dashboard/summary",
    "/dashboard/utilization",
    "/dashboard/cutting-ratio",
    "/dashboard/status-distribution",
    "/dashboard/daily-trend",
    "/alarms",
    "/alarms?severity=CRITICAL",
    "/rollup/hourly?date=2026-01-01",
    "/rollup/daily?year=2026&month=1",
    "/rollup/monthly?year=2026",
    "/machines/history?machineId=CNC-DEMO-01&page=0&size=5",
    "/prealarm/summary",
    "/prealarm/indicators"
)

$failed = 0

foreach ($endpoint in $endpoints) {
    $url = "$BaseUrl$endpoint"
    try {
        $response = Invoke-RestMethod -Uri $url -Method Get
        if ($response.success -eq $true) {
            Write-Host "[PASS] $endpoint"
        } else {
            $failed++
            Write-Host "[FAIL] $endpoint - API wrapper success was not true"
        }
    } catch {
        $failed++
        Write-Host "[FAIL] $endpoint - $($_.Exception.Message)"
    }
}

if ($failed -gt 0) {
    Write-Host ""
    Write-Host "$failed endpoint test(s) failed."
    exit 1
}

Write-Host ""
Write-Host "All backend API smoke tests passed."
