# Script para crear un usuario de prueba
Write-Host "🔧 Creando usuario de prueba..." -ForegroundColor Green

# Datos del usuario de prueba
$userData = @{
    organizationId = "00000000-0000-0000-0000-000000000000"  # UUID por defecto
    name = "Super Admin"
    email = "admin@cadetex.com"
    password = "admin123"
    role = "SUPERADMIN"
} | ConvertTo-Json

Write-Host "📝 Datos del usuario:" -ForegroundColor Yellow
Write-Host $userData

Write-Host "🚀 Enviando request al backend..." -ForegroundColor Blue

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/auth/register" -Method POST -Body $userData -ContentType "application/json"
    Write-Host "✅ Usuario creado exitosamente!" -ForegroundColor Green
    Write-Host "📧 Email: admin@cadetex.com" -ForegroundColor Cyan
    Write-Host "🔑 Password: admin123" -ForegroundColor Cyan
    Write-Host "👤 Role: SUPERADMIN" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Error al crear usuario:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}

Write-Host "`n🎯 Ahora puedes usar estas credenciales para hacer login en el frontend" -ForegroundColor Green
