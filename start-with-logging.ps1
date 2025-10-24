# Script para iniciar el backend con logging habilitado
Write-Host "Iniciando Cadetex Backend con logging habilitado..." -ForegroundColor Green

# Crear directorio de logs si no existe
if (!(Test-Path "logs")) {
    New-Item -ItemType Directory -Path "logs"
    Write-Host "Directorio de logs creado" -ForegroundColor Yellow
}

# Iniciar el backend
Write-Host "Iniciando servidor..." -ForegroundColor Cyan
./gradlew bootRun
