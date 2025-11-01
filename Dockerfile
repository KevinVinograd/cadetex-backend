# Build stage
FROM gradle:8.10.2-jdk17 AS build

WORKDIR /app

# Copiar archivos de Gradle (y la carpeta completa con wrappers y scripts)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./

# Descargar dependencias (cachea si no cambian los archivos anteriores)
RUN gradle dependencies --no-daemon || true

# Copiar código fuente
COPY src ./src

# Construir el JAR
RUN gradle clean shadowJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Crear usuario no-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el JAR desde el build stage
COPY --from=build /app/build/libs/*all.jar app.jar

# Crear carpetas y permisos
RUN mkdir -p /app/config /app/logs && chown -R appuser:appgroup /app

USER appuser
EXPOSE 8080

# JVM tuning (2GB RAM aprox)
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dconfig.file=/app/config/application.conf -jar app.jar"]
