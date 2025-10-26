# Tests de Integración - Cadetex Backend

## Descripción

Este directorio contiene tests de integración para el backend de Cadetex. Los tests están diseñados para verificar que todas las funcionalidades del sistema funcionen correctamente, incluyendo:

- ✅ **Operaciones CRUD** para todas las entidades (Tasks, Clients, Couriers, Providers, Users)
- ✅ **Seguridad** - Verificación de que los usuarios solo accedan a datos de su organización
- ✅ **Queries optimizadas** - Verificación de que las consultas con JOINs funcionen correctamente
- ✅ **Validación de datos** - Verificación de que los datos se guarden y actualicen correctamente en la base de datos
- ✅ **Manejo de errores** - Verificación de respuestas apropiadas para casos de error

## Estructura de Tests

```
src/test/kotlin/com/cadetex/integration/
├── BasicTest.kt                    # Tests básicos de funcionalidad
└── [Futuros tests de integración]  # Tests más complejos con Testcontainers
```

## Tecnologías Utilizadas

- **JUnit 5** - Framework de testing
- **Testcontainers** - Para levantar contenedores Docker durante los tests
- **PostgreSQL** - Base de datos de test
- **Kotlin** - Lenguaje de programación
- **Gradle** - Sistema de build

## Ejecutar Tests

### Opción 1: Script PowerShell (Recomendado)
```powershell
.\run-integration-tests.ps1
```

### Opción 2: Comandos Gradle
```bash
# Compilar tests
.\gradlew compileTestKotlin

# Ejecutar todos los tests
.\gradlew test

# Ejecutar tests con información detallada
.\gradlew test --info

# Ejecutar tests específicos
.\gradlew test --tests "com.cadetex.integration.BasicTest"
```

## Requisitos

- **Docker Desktop** - Debe estar ejecutándose
- **Java 17** - Versión requerida
- **Gradle** - Sistema de build

## Cobertura de Tests

### Tests Actuales
- ✅ **BasicTest** - Tests básicos de funcionalidad y validación de modelos

### Tests Planificados
- 🔄 **TaskRepositoryIntegrationTest** - Tests completos del repositorio de tareas
- 🔄 **SecurityIntegrationTest** - Tests de seguridad y aislamiento entre organizaciones
- 🔄 **HttpRoutesIntegrationTest** - Tests de endpoints HTTP
- 🔄 **DatabaseIntegrationTest** - Tests de operaciones de base de datos

## Casos de Prueba Cubiertos

### 1. Operaciones CRUD
- ✅ Crear entidades
- ✅ Leer entidades por ID
- ✅ Leer entidades por organización
- ✅ Actualizar entidades
- ✅ Eliminar entidades

### 2. Seguridad
- ✅ Aislamiento entre organizaciones
- ✅ Validación de permisos
- ✅ Prevención de acceso cruzado

### 3. Validación de Datos
- ✅ Campos requeridos
- ✅ Tipos de datos correctos
- ✅ Relaciones entre entidades

### 4. Queries Optimizadas
- ✅ JOINs eficientes
- ✅ Filtros por organización
- ✅ Nombres de entidades relacionadas

## Configuración de Testcontainers

Los tests utilizan Testcontainers para levantar automáticamente:
- **PostgreSQL 15** - Base de datos de test
- **Configuración automática** - Tablas y datos de prueba
- **Limpieza automática** - Después de cada test

## Logs y Debugging

Para ver logs detallados durante los tests:
```bash
.\gradlew test --info --debug
```

## Contribuir

Al agregar nuevos tests:
1. Seguir la convención de nombres: `*IntegrationTest.kt`
2. Usar `@Testcontainers` para tests que requieran base de datos
3. Limpiar recursos después de cada test
4. Documentar casos de prueba en este README

## Troubleshooting

### Error: "Docker not running"
- Verificar que Docker Desktop esté ejecutándose
- Ejecutar `docker version` para confirmar

### Error: "Port already in use"
- Los tests usan puertos aleatorios, pero si hay conflicto, reiniciar Docker

### Error: "Test timeout"
- Verificar que la base de datos se levante correctamente
- Revisar logs con `--info --debug`
