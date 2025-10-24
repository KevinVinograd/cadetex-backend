# Script simple para ejecutar tests
Write-Host "🧪 Ejecutando tests de integración de Cadetex..." -ForegroundColor Green

# Verificar que Docker esté ejecutándose
try {
    docker version | Out-Null
    Write-Host "✅ Docker está ejecutándose" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker no está ejecutándose. Por favor inicia Docker Desktop" -ForegroundColor Red
    exit 1
}

# Ejecutar tests
Write-Host "🚀 Ejecutando tests..." -ForegroundColor Yellow
.\gradlew.bat test --info

# Mostrar resultados
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Todos los tests pasaron exitosamente!" -ForegroundColor Green
    Write-Host "📊 Reporte disponible en: build/reports/tests/test/index.html" -ForegroundColor Cyan
} else {
    Write-Host "❌ Algunos tests fallaron. Revisa el output anterior." -ForegroundColor Red
}



