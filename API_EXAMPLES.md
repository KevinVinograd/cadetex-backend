# Cadetex API - Ejemplos de Uso

## 🔐 Autenticación

### 1. Registrar un usuario
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Juan Pérez",
    "email": "juan@example.com",
    "password": "password123",
    "role": "ORGADMIN"
  }'
```

### 2. Iniciar sesión
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "password123"
  }'
```

### 3. Usar el token JWT
```bash
# Guardar el token de la respuesta anterior
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Usar el token en requests posteriores
curl -X GET http://localhost:8080/users \
  -H "Authorization: Bearer $TOKEN"
```

## 🏢 Organizaciones

### Crear organización (solo SUPERADMIN)
```bash
curl -X POST http://localhost:8080/organizations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mi Empresa S.A."
  }'
```

### Listar organizaciones
```bash
curl -X GET http://localhost:8080/organizations \
  -H "Authorization: Bearer $TOKEN"
```

## 👥 Usuarios

### Crear usuario
```bash
curl -X POST http://localhost:8080/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174000",
    "name": "María García",
    "email": "maria@example.com",
    "password": "password123",
    "role": "COURIER"
  }'
```

### Listar usuarios
```bash
curl -X GET http://localhost:8080/users \
  -H "Authorization: Bearer $TOKEN"
```

## 📦 Tareas

### Crear tarea
```bash
curl -X POST http://localhost:8080/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174000",
    "type": "PICKUP",
    "referenceNumber": "REF-001",
    "clientId": "123e4567-e89b-12d3-a456-426614174001",
    "providerId": "123e4567-e89b-12d3-a456-426614174002",
    "courierId": "123e4567-e89b-12d3-a456-426614174003",
    "status": "PENDING",
    "priority": "NORMAL",
    "scheduledDate": "2024-01-15",
    "notes": "Recoger paquete en oficina principal"
  }'
```

### Listar tareas
```bash
curl -X GET http://localhost:8080/tasks \
  -H "Authorization: Bearer $TOKEN"
```

### Obtener tarea por ID
```bash
curl -X GET http://localhost:8080/tasks/123e4567-e89b-12d3-a456-426614174004 \
  -H "Authorization: Bearer $TOKEN"
```

## 🚚 Couriers

### Crear courier
```bash
curl -X POST http://localhost:8080/couriers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Carlos López",
    "phoneNumber": "+54911234567",
    "email": "carlos@example.com",
    "address": "Av. Corrientes 1234, CABA",
    "vehicleType": "Moto"
  }'
```

### Listar couriers
```bash
curl -X GET http://localhost:8080/couriers \
  -H "Authorization: Bearer $TOKEN"
```

## 🏢 Clientes

### Crear cliente
```bash
curl -X POST http://localhost:8080/clients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Empresa Cliente S.A.",
    "address": "Av. Santa Fe 5678, CABA",
    "city": "Buenos Aires",
    "province": "CABA",
    "contactName": "Ana Martínez",
    "contactPhone": "+54911234568"
  }'
```

## 🏭 Proveedores

### Crear proveedor
```bash
curl -X POST http://localhost:8080/providers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Proveedor Logística S.A.",
    "address": "Av. Rivadavia 9012, CABA",
    "city": "Buenos Aires",
    "province": "CABA",
    "contactName": "Roberto Silva",
    "contactPhone": "+54911234569"
  }'
```

## 📸 Fotos de Tareas

### Subir foto de tarea
```bash
curl -X POST http://localhost:8080/task-photos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "123e4567-e89b-12d3-a456-426614174004",
    "photoUrl": "https://example.com/photo.jpg",
    "description": "Foto del paquete entregado"
  }'
```

## 📊 Historial de Tareas

### Crear entrada de historial
```bash
curl -X POST http://localhost:8080/task-history \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "123e4567-e89b-12d3-a456-426614174004",
    "previousStatus": "PENDING",
    "newStatus": "IN_PROGRESS",
    "changedBy": "Carlos López"
  }'
```

## 🔍 Búsquedas

### Buscar tareas por courier
```bash
curl -X GET "http://localhost:8080/tasks/courier/123e4567-e89b-12d3-a456-426614174003" \
  -H "Authorization: Bearer $TOKEN"
```

### Buscar tareas por estado
```bash
curl -X GET "http://localhost:8080/tasks/status/PENDING" \
  -H "Authorization: Bearer $TOKEN"
```

### Buscar clientes por nombre
```bash
curl -X GET "http://localhost:8080/clients/search?name=Empresa" \
  -H "Authorization: Bearer $TOKEN"
```

## 🚨 Códigos de Error Comunes

- **400 Bad Request**: Error en la validación de datos
- **401 Unauthorized**: Token JWT inválido o expirado
- **403 Forbidden**: Sin permisos para realizar la acción
- **404 Not Found**: Recurso no encontrado
- **409 Conflict**: Conflicto (ej: usuario ya existe)

## 📝 Notas Importantes

1. **Autenticación**: Todos los endpoints (excepto `/auth/*`) requieren un token JWT válido
2. **Roles**: 
   - SUPERADMIN: Acceso completo
   - ORGADMIN: Solo su organización
   - COURIER: Solo sus tareas asignadas
3. **UUIDs**: Todos los IDs son UUIDs en formato string
4. **Fechas**: Formato ISO 8601 (YYYY-MM-DD para fechas, YYYY-MM-DDTHH:mm:ss para timestamps)
5. **Validación**: Los datos se validan automáticamente según las reglas definidas

