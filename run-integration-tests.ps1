# Script para ejecutar tests de integración
Write-Host "=== EJECUTANDO TESTS DE INTEGRACIÓN ===" -ForegroundColor Green

# Verificar que Docker esté ejecutándose
Write-Host "Verificando Docker..." -ForegroundColor Yellow
try {
    docker version | Out-Null
    Write-Host "✓ Docker está ejecutándose" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker no está ejecutándose. Por favor inicia Docker Desktop." -ForegroundColor Red
    exit 1
}

# Compilar el proyecto
Write-Host "Compilando proyecto..." -ForegroundColor Yellow
.\gradlew compileKotlin
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Error en la compilación" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Compilación exitosa" -ForegroundColor Green

# Ejecutar tests
Write-Host "Ejecutando tests..." -ForegroundColor Yellow
.\gradlew test --info
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Algunos tests fallaron" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Todos los tests pasaron" -ForegroundColor Green

Write-Host "=== TESTS COMPLETADOS ===" -ForegroundColor Green
