# Script para monitorear logs en tiempo real
Write-Host "Monitoreando logs del backend..." -ForegroundColor Green
Write-Host "Presiona Ctrl+C para salir" -ForegroundColor Yellow
Write-Host ""

# Verificar si existen los archivos de log
$errorLog = "logs/errors.log"
$apiLog = "logs/api.log"

if (Test-Path $errorLog) {
    Write-Host "=== ERROR LOGS ===" -ForegroundColor Red
    Get-Content $errorLog -Wait -Tail 10
} else {
    Write-Host "Archivo de error log no encontrado: $errorLog" -ForegroundColor Yellow
}

if (Test-Path $apiLog) {
    Write-Host "=== API LOGS ===" -ForegroundColor Blue
    Get-Content $apiLog -Wait -Tail 10
} else {
    Write-Host "Archivo de API log no encontrado: $apiLog" -ForegroundColor Yellow
}
