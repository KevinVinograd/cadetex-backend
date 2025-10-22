# Script para detener la base de datos PostgreSQL
Write-Host "🛑 Deteniendo base de datos PostgreSQL..." -ForegroundColor Yellow

# Detener los servicios
docker-compose down

Write-Host "✅ Base de datos PostgreSQL detenida correctamente!" -ForegroundColor Green
