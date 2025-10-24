# Script simple para crear usuario de prueba
$userData = @{
    organizationId = "00000000-0000-0000-0000-000000000000"
    name = "Super Admin"
    email = "admin@cadetex.com"
    password = "admin123"
    role = "SUPERADMIN"
} | ConvertTo-Json

Write-Host "Creando usuario de prueba..." -ForegroundColor Green
Write-Host "Email: admin@cadetex.com" -ForegroundColor Cyan
Write-Host "Password: admin123" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/auth/register" -Method POST -Body $userData -ContentType "application/json"
    Write-Host "✅ Usuario creado exitosamente!" -ForegroundColor Green
    Write-Host "Token: $($response.token)" -ForegroundColor Yellow
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}



