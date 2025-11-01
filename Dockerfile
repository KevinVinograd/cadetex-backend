# Build stage
FROM gradle:8.10.2-jdk17 AS build

WORKDIR /app

# Copiar archivos de Gradle primero (para aprovechar cache)
COPY build.gradle.kts settings.gradle.kts gradle/ ./
COPY gradlew gradlew.bat ./

# Descargar dependencias (se cachea si no cambian los archivos de configuración)
RUN gradle dependencies --no-daemon || true

# Copiar código fuente
COPY src/ ./src/

# Construir el JAR
RUN gradle clean shadowJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar JAR desde build stage
COPY --from=build /app/build/libs/cadetex-backend-v2-all.jar app.jar

# Crear directorio para logs y config
RUN mkdir -p /app/config /app/logs && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Variables de entorno por defecto (optimizado para t3.small con 2GB RAM)
# -Xmx512m: máximo heap (deja ~1.2GB para sistema + Docker)
# -Xms256m: inicial heap
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dconfig.file=/app/config/application.conf -jar app.jar"]

