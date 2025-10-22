# Script para levantar la base de datos PostgreSQL con Docker
Write-Host "🚀 Iniciando base de datos PostgreSQL con Docker..." -ForegroundColor Green

# Verificar si Docker está ejecutándose
try {
    docker version | Out-Null
    Write-Host "✅ Docker está ejecutándose" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker no está ejecutándose. Por favor inicia Docker Desktop" -ForegroundColor Red
    exit 1
}

# Levantar los servicios
Write-Host "📦 Levantando servicios de base de datos..." -ForegroundColor Yellow
docker-compose up -d

# Esperar a que la base de datos esté lista
Write-Host "⏳ Esperando a que la base de datos esté lista..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Verificar el estado de los contenedores
Write-Host "🔍 Verificando estado de los contenedores..." -ForegroundColor Yellow
docker-compose ps

Write-Host "✅ Base de datos PostgreSQL iniciada correctamente!" -ForegroundColor Green
Write-Host "📊 PgAdmin disponible en: http://localhost:5050" -ForegroundColor Cyan
Write-Host "   Email: admin@cadetex.com" -ForegroundColor Cyan
Write-Host "   Password: admin123" -ForegroundColor Cyan
Write-Host "🗄️ PostgreSQL disponible en: localhost:5432" -ForegroundColor Cyan
Write-Host "   Database: cadetex" -ForegroundColor Cyan
Write-Host "   User: cadetex_user" -ForegroundColor Cyan
Write-Host "   Password: cadetex_password" -ForegroundColor Cyan
