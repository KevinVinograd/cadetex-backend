# Cadetex Backend

Sistema de gestión de cadetes - Backend API

## 🚀 Inicio Rápido

### 1. Levantar Base de Datos con Docker

```bash
# Navegar al directorio del proyecto
cd cadetex-backend

# Levantar PostgreSQL y PgAdmin
docker-compose up -d

# Verificar que estén ejecutándose
docker-compose ps
```

### 2. Conectarse a la Base de Datos

#### Opción A: PgAdmin (Interfaz Web)
1. Abrir http://localhost:5050
2. Iniciar sesión:
   - **Email**: `admin@cadetex.com`
   - **Contraseña**: `admin123`
3. Agregar servidor:
   - **Host**: `127.0.0.1`
   - **Puerto**: `5432`
   - **Base de datos**: `cadetex`
   - **Usuario**: `cadetex_user`
   - **Contraseña**: `cadetex_password`

#### Opción B: Línea de Comandos
```bash
# Conectar directamente a PostgreSQL
docker exec -it cadetex-postgres psql -U cadetex_user -d cadetex
```

### 3. Ejecutar la Aplicación

```bash
# Compilar y ejecutar
./gradlew build
./gradlew run

# O ejecutar JAR
java -jar build/libs/cadetex-backend-v2-all.jar
```

### 4. Acceder a la API

- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger
- **OpenAPI YAML**: http://localhost:8080/openapi/documentation.yaml

## 🛠️ Tecnologías

- **Kotlin** 2.1.0
- **Ktor** 3.2.2
- **Exposed** 0.56.0 (ORM)
- **PostgreSQL** 15
- **JWT** para autenticación
- **BCrypt** para hash de contraseñas
- **Docker** para contenedores
- **Swagger/OpenAPI** para documentación

## 📁 Estructura del Proyecto

```
src/main/kotlin/com/cadetex/
├── auth/                    # Autenticación JWT
├── database/
│   └── tables/             # Definiciones de tablas Exposed
├── model/                  # Modelos de datos
├── repository/             # Repositorios de datos
├── routes/                 # Rutas de la API
├── validation/             # Middleware de validación
├── Application.kt          # Configuración principal
├── Databases.kt           # Configuración de base de datos
├── Routing.kt             # Configuración de rutas
└── Serialization.kt       # Configuración de serialización
```

## 🗄️ Base de Datos

### Configuración de PostgreSQL

- **Host**: `127.0.0.1`
- **Puerto**: `5432`
- **Base de datos**: `cadetex`
- **Usuario**: `cadetex_user`
- **Contraseña**: `cadetex_password`

### Scripts de Gestión

```bash
# Levantar base de datos
.\start-database.ps1

# Detener base de datos
.\stop-database.ps1

# Ver logs de PostgreSQL
docker logs cadetex-postgres

# Ver logs de PgAdmin
docker logs cadetex-pgadmin
```

### Estructura de Tablas

- `organizations` - Organizaciones
- `users` - Usuarios del sistema
- `clients` - Clientes
- `providers` - Proveedores
- `couriers` - Cadetes/couriers
- `tasks` - Tareas
- `task_photos` - Fotos de tareas
- `task_history` - Historial de tareas

## 🔐 Autenticación

### Usuarios de Prueba

| Email | Contraseña | Rol | Descripción |
|-------|------------|-----|-------------|
| `admin@cadetex.com` | `admin123` | SUPERADMIN | Administrador del sistema |
| `orgadmin@cadetex.com` | `orgadmin123` | ORGADMIN | Administrador de organización |
| `courier@cadetex.com` | `courier123` | COURIER | Cadete/courier |

### Uso de JWT

```bash
# 1. Iniciar sesión
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@cadetex.com", "password": "admin123"}'

# 2. Usar el token en requests
curl -X GET http://localhost:8080/users \
  -H "Authorization: Bearer <tu-token-jwt>"
```

## 📚 Documentación de la API

### Swagger UI
Accede a http://localhost:8080/swagger para:
- Ver todos los endpoints disponibles
- Probar la API directamente desde el navegador
- Ver esquemas de datos
- Ejemplos de requests y responses

### Endpoints Principales

#### 🔐 Autenticación
- `POST /auth/register` - Registrar usuario
- `POST /auth/login` - Iniciar sesión
- `POST /auth/validate` - Validar token

#### 🏢 Organizaciones
- `GET /organizations` - Listar organizaciones
- `POST /organizations` - Crear organización
- `GET /organizations/{id}` - Obtener organización
- `PUT /organizations/{id}` - Actualizar organización
- `DELETE /organizations/{id}` - Eliminar organización

#### 👥 Usuarios
- `GET /users` - Listar usuarios
- `POST /users` - Crear usuario
- `GET /users/{id}` - Obtener usuario
- `PUT /users/{id}` - Actualizar usuario
- `DELETE /users/{id}` - Eliminar usuario

#### 📦 Tareas
- `GET /tasks` - Listar tareas
- `POST /tasks` - Crear tarea
- `GET /tasks/{id}` - Obtener tarea
- `PUT /tasks/{id}` - Actualizar tarea
- `DELETE /tasks/{id}` - Eliminar tarea
- `GET /tasks/courier/{courierId}` - Tareas por courier
- `GET /tasks/status/{status}` - Tareas por estado

#### 🚚 Couriers
- `GET /couriers` - Listar couriers
- `POST /couriers` - Crear courier
- `GET /couriers/{id}` - Obtener courier
- `PUT /couriers/{id}` - Actualizar courier
- `DELETE /couriers/{id}` - Eliminar courier

#### 🏢 Clientes
- `GET /clients` - Listar clientes
- `POST /clients` - Crear cliente
- `GET /clients/{id}` - Obtener cliente
- `PUT /clients/{id}` - Actualizar cliente
- `DELETE /clients/{id}` - Eliminar cliente

#### 🏭 Proveedores
- `GET /providers` - Listar proveedores
- `POST /providers` - Crear proveedor
- `GET /providers/{id}` - Obtener proveedor
- `PUT /providers/{id}` - Actualizar proveedor
- `DELETE /providers/{id}` - Eliminar proveedor

## 🧪 Testing

### Tests de Integración

El proyecto incluye tests de integración completos que:

- **Levantan PostgreSQL en Docker** automáticamente para cada test
- **Inicializan datos de prueba** en cada ejecución
- **Testean endpoints completos** con autenticación JWT
- **Verifican lógica de negocio** y flujos de trabajo
- **Validan control de acceso** por roles de usuario

### Ejecutar Tests

```bash
# Ejecutar todos los tests (recomendado)
.\run-tests-simple.ps1

# O ejecutar manualmente con Gradle
./gradlew test

# Ejecutar tests con reporte detallado
./gradlew test --info

# Ejecutar tests con reporte HTML
./gradlew testWithReport

# Ver reporte HTML de tests
# Abrir: build/reports/tests/test/index.html
```

### ¿Qué hace automáticamente?

- ✅ **Verifica Docker** está ejecutándose
- 🐳 **Levanta PostgreSQL** en contenedor para tests
- 🧪 **Ejecuta todos los tests** de integración
- 📊 **Genera reporte HTML** con resultados
- 🧹 **Limpia contenedores** al finalizar

### Estado Actual de Tests

- ✅ **Tests básicos** funcionando correctamente
- ✅ **Configuración de Gradle** con Docker integrado
- ✅ **Reportes HTML** generados automáticamente
- 🔄 **Tests de integración** pendientes de implementación

### Integración Frontend-Backend

- ✅ **API Service** configurado para comunicación
- ✅ **Autenticación JWT** integrada
- ✅ **Login** conectado con backend real
- ✅ **Organizaciones** con CRUD completo
- 🔄 **Clientes, Proveedores, Couriers** en progreso

### Crear Usuario de Prueba

```bash
# Ejecutar script para crear usuario de prueba
.\create-test-user.ps1

# Credenciales de prueba:
# Email: admin@cadetex.com
# Password: admin123
# Role: SUPERADMIN
```

### Tipos de Tests

#### 🔐 Tests de Autenticación
- Registro de usuarios
- Login y validación de tokens
- Manejo de errores de autenticación
- Validación de datos de entrada

#### 🏢 Tests de Organizaciones
- CRUD completo de organizaciones
- Control de acceso por roles
- Validación de permisos
- Manejo de errores

#### 📦 Tests de Tareas
- Creación y gestión de tareas
- Asignación a couriers
- Cambios de estado
- Búsqueda y filtrado
- Flujos de trabajo completos

#### 🚚 Tests de Couriers
- Gestión de couriers
- Búsqueda por nombre y teléfono
- Filtrado por organización
- Estados activo/inactivo

#### 🔄 Tests de Lógica de Negocio
- Flujos de trabajo completos (PICKUP -> DELIVERY)
- Transiciones de estado de tareas
- Gestión de prioridades
- Historial de cambios
- Búsquedas y filtros complejos

### Configuración de Tests

Los tests utilizan:
- **Testcontainers** para PostgreSQL en Docker
- **Ktor Test Engine** para testing de endpoints
- **JUnit 5** con ejecución paralela
- **Datos de prueba** generados automáticamente

### Ejemplos de Uso

Ver `API_EXAMPLES.md` para ejemplos completos de uso de la API con curl.

### Datos de Prueba

La base de datos se inicializa automáticamente con:
- 1 organización demo
- 3 usuarios de prueba (SUPERADMIN, ORGADMIN, COURIER)
- 1 cliente demo
- 1 proveedor demo
- 1 courier demo

## 🐳 Docker

### Comandos Útiles

```bash
# Ver estado de contenedores
docker-compose ps

# Ver logs
docker-compose logs postgres
docker-compose logs pgadmin

# Reiniciar servicios
docker-compose restart

# Detener y eliminar contenedores
docker-compose down

# Detener y eliminar contenedores + volúmenes
docker-compose down -v
```

### Volúmenes

- `postgres_data`: Datos persistentes de PostgreSQL
- Los datos se mantienen entre reinicios del contenedor

## 🔧 Desarrollo

### Instalación

```bash
# Clonar repositorio
git clone <repository-url>
cd cadetex-backend

# Instalar dependencias
./gradlew build
```

### Estructura de Código

- **Models**: Definiciones de datos y DTOs
- **Tables**: Esquemas de base de datos con Exposed
- **Repositories**: Lógica de acceso a datos
- **Routes**: Endpoints de la API
- **Auth**: Autenticación JWT
- **Validation**: Middleware de validación

### Scripts de Desarrollo

```bash
# Compilar
./gradlew build

# Ejecutar tests
./gradlew test

# Generar JAR
./gradlew shadowJar

# Limpiar build
./gradlew clean
```

## 🚨 Solución de Problemas

### PostgreSQL no se conecta

1. Verificar que Docker esté ejecutándose
2. Verificar que los contenedores estén activos: `docker-compose ps`
3. Verificar logs: `docker logs cadetex-postgres`
4. Probar conexión: `Test-NetConnection -ComputerName 127.0.0.1 -Port 5432`

### PgAdmin no se conecta

1. Usar `127.0.0.1` en lugar de `localhost`
2. Verificar que el puerto 5432 esté expuesto
3. Verificar logs: `docker logs cadetex-pgadmin`

### Aplicación no inicia

1. Verificar que PostgreSQL esté ejecutándose
2. Verificar configuración en `application.conf`
3. Verificar logs de la aplicación

## 📝 Contribuir

1. Fork el proyecto
2. Crear una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abrir un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👨‍💻 Autor

**Kevin Vinograd**
- GitHub: [@KevinVinograd](https://github.com/KevinVinograd)

## 📞 Soporte

Si tienes alguna pregunta o problema, por favor abre un issue en el repositorio.