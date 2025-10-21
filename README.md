# Cadetex Backend

Sistema de gestión de cadetes - Backend API desarrollado con Kotlin y Ktor.

## 🚀 Características

- **API REST completa** para gestión de organizaciones, usuarios, clientes, proveedores, couriers y tareas
- **Autenticación JWT** con hash de contraseñas BCrypt
- **Control de acceso basado en roles** (SUPERADMIN, ORGADMIN, COURIER)
- **Base de datos H2** con ORM Exposed
- **Validación de datos** automática
- **CORS configurado** para desarrollo frontend
- **Logging completo** de requests

## 🛠️ Tecnologías

- **Kotlin 2.1.0** - Lenguaje principal
- **Ktor 3.2.2** - Framework web
- **Exposed 0.56.0** - ORM para base de datos
- **H2 Database** - Base de datos en memoria
- **JWT + BCrypt** - Autenticación y seguridad
- **Gradle 8.5** - Build tool

## 📋 Entidades

- **Organizations** - Gestión de organizaciones
- **Users** - Usuarios del sistema con roles
- **Clients** - Clientes de las organizaciones
- **Providers** - Proveedores de servicios
- **Couriers** - Cadetes/couriers
- **Tasks** - Tareas asignadas a couriers
- **TaskPhotos** - Fotos asociadas a tareas
- **TaskHistory** - Historial de cambios de tareas

## 🔐 Autenticación

### Endpoints públicos:
- `POST /auth/register` - Registro de usuarios
- `POST /auth/login` - Login de usuarios
- `POST /auth/validate` - Validación de tokens (requiere JWT)

### Headers requeridos para endpoints protegidos:
```
Authorization: Bearer <jwt_token>
```

## 🚀 Instalación y Ejecución

### Prerrequisitos
- Java 17 o superior
- Gradle 8.5

### Ejecutar la aplicación

```bash
# Compilar el proyecto
./gradlew build

# Ejecutar la aplicación
./gradlew run
```

La API estará disponible en `http://localhost:8080`

### Ejecutar con JAR

```bash
# Generar JAR ejecutable
./gradlew shadowJar

# Ejecutar JAR
java -jar build/libs/cadetex-backend-v2-all.jar
```

## 📚 Endpoints de la API

### Organizaciones
- `GET /organizations` - Listar organizaciones
- `POST /organizations` - Crear organización
- `GET /organizations/{id}` - Obtener organización
- `PUT /organizations/{id}` - Actualizar organización
- `DELETE /organizations/{id}` - Eliminar organización

### Usuarios
- `GET /users` - Listar usuarios
- `POST /users` - Crear usuario
- `GET /users/{id}` - Obtener usuario
- `PUT /users/{id}` - Actualizar usuario
- `DELETE /users/{id}` - Eliminar usuario
- `GET /users/organization/{orgId}` - Usuarios por organización
- `GET /users/role/{role}` - Usuarios por rol

### Clientes
- `GET /clients` - Listar clientes
- `POST /clients` - Crear cliente
- `GET /clients/{id}` - Obtener cliente
- `PUT /clients/{id}` - Actualizar cliente
- `DELETE /clients/{id}` - Eliminar cliente
- `GET /clients/organization/{orgId}` - Clientes por organización
- `GET /clients/search?name={name}` - Buscar clientes por nombre
- `GET /clients/search?city={city}` - Buscar clientes por ciudad

### Proveedores
- `GET /providers` - Listar proveedores
- `POST /providers` - Crear proveedor
- `GET /providers/{id}` - Obtener proveedor
- `PUT /providers/{id}` - Actualizar proveedor
- `DELETE /providers/{id}` - Eliminar proveedor
- `GET /providers/organization/{orgId}` - Proveedores por organización
- `GET /providers/search?name={name}` - Buscar proveedores por nombre
- `GET /providers/search?city={city}` - Buscar proveedores por ciudad

### Couriers
- `GET /couriers` - Listar couriers
- `POST /couriers` - Crear courier
- `GET /couriers/{id}` - Obtener courier
- `PUT /couriers/{id}` - Actualizar courier
- `DELETE /couriers/{id}` - Eliminar courier
- `GET /couriers/organization/{orgId}` - Couriers por organización
- `GET /couriers/active/{orgId}` - Couriers activos por organización
- `GET /couriers/search?name={name}` - Buscar couriers por nombre
- `GET /couriers/search?phone={phone}` - Buscar couriers por teléfono

### Tareas
- `GET /tasks` - Listar tareas
- `POST /tasks` - Crear tarea
- `GET /tasks/{id}` - Obtener tarea
- `PUT /tasks/{id}` - Actualizar tarea
- `DELETE /tasks/{id}` - Eliminar tarea
- `GET /tasks/organization/{orgId}` - Tareas por organización
- `GET /tasks/courier/{courierId}` - Tareas por courier
- `GET /tasks/status/{status}` - Tareas por estado
- `GET /tasks/search?reference={ref}` - Buscar tareas por referencia

### Fotos de Tareas
- `GET /task-photos` - Listar fotos
- `POST /task-photos` - Crear foto
- `GET /task-photos/{id}` - Obtener foto
- `PUT /task-photos/{id}` - Actualizar foto
- `DELETE /task-photos/{id}` - Eliminar foto
- `GET /task-photos/task/{taskId}` - Fotos por tarea

### Historial de Tareas
- `GET /task-history` - Listar historial
- `POST /task-history` - Crear entrada de historial
- `GET /task-history/{id}` - Obtener entrada de historial
- `PUT /task-history/{id}` - Actualizar entrada de historial
- `DELETE /task-history/{id}` - Eliminar entrada de historial
- `GET /task-history/task/{taskId}` - Historial por tarea

## 🔒 Control de Acceso

### Roles del Sistema:
- **SUPERADMIN**: Acceso completo a todas las organizaciones y datos
- **ORGADMIN**: Acceso limitado a su organización
- **COURIER**: Acceso limitado a sus tareas asignadas

### Reglas de Acceso:
- Los SUPERADMIN pueden ver y gestionar todo
- Los ORGADMIN solo pueden ver datos de su organización
- Los COURIER solo pueden ver sus tareas asignadas
- Todos los endpoints requieren autenticación JWT excepto `/auth/*`

## 🧪 Testing

```bash
# Ejecutar tests
./gradlew test

# Ejecutar tests con reporte de cobertura
./gradlew test jacocoTestReport
```

## 📝 Estructura del Proyecto

```
src/main/kotlin/com/cadetex/
├── auth/                    # Autenticación JWT
├── database/tables/         # Definiciones de tablas Exposed
├── model/                   # Modelos de datos
├── repository/              # Repositorios de datos
├── routes/                  # Definiciones de rutas API
├── validation/              # Validación de datos
├── Application.kt           # Configuración principal
├── CORS.kt                  # Configuración CORS
├── Databases.kt             # Configuración de base de datos
├── Logging.kt               # Configuración de logging
└── Routing.kt               # Configuración de rutas
```

## 🤝 Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👨‍💻 Autor

**Kevin Vinograd**
- GitHub: [@KevinVinograd](https://github.com/KevinVinograd)

## 📞 Soporte

Si tienes alguna pregunta o problema, por favor abre un issue en el repositorio.
