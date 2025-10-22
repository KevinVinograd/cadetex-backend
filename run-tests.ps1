# Script para ejecutar tests de integración
Write-Host "🧪 Ejecutando tests de integración de Cadetex..." -ForegroundColor Green

# Verificar que Docker esté ejecutándose
try {
    docker version | Out-Null
    Write-Host "✅ Docker está ejecutándose" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker no está ejecutándose. Por favor inicia Docker Desktop" -ForegroundColor Red
    exit 1
}

# Verificar que PostgreSQL esté ejecutándose
Write-Host "🔍 Verificando que PostgreSQL esté ejecutándose..." -ForegroundColor Yellow
$postgresRunning = docker ps --filter "name=cadetex-postgres" --format "{{.Names}}"
if ($postgresRunning -notcontains "cadetex-postgres") {
    Write-Host "📦 Levantando PostgreSQL para tests..." -ForegroundColor Yellow
    docker-compose up -d postgres
    Start-Sleep -Seconds 10
}

# Ejecutar tests
Write-Host "🚀 Ejecutando tests..." -ForegroundColor Yellow
.\gradlew.bat test --info

# Mostrar resultados
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Todos los tests pasaron exitosamente!" -ForegroundColor Green
} else {
    Write-Host "❌ Algunos tests fallaron. Revisa el output anterior." -ForegroundColor Red
}

Write-Host "📊 Reporte de tests disponible en: build/reports/tests/test/index.html" -ForegroundColor Cyan


