# FinPilot AI - Backend Launcher
# Run this script from the backend/ directory to start the Spring Boot server
# Usage: .\run-backend.ps1

Write-Host "=== FinPilot AI Backend Launcher ===" -ForegroundColor Cyan
Write-Host "Loading environment variables from .env..." -ForegroundColor Yellow

# Add MySQL 9.4 to PATH for this session
$env:Path += ";C:\Program Files\MySQL\MySQL Server 9.4\bin"

# Load .env file
Get-Content ".env" | ForEach-Object {
    if ($_ -match '^(?<name>[^#\s][^=]*)=(?<value>.*)$') {
        $name = $Matches.name.Trim()
        $value = $Matches.value.Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        Set-Item -Path "Env:\$name" -Value $value
    }
}

Write-Host "Starting Spring Boot on port $env:SERVER_PORT (default: 8080)..." -ForegroundColor Green
mvn spring-boot:run
