plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.ktor.plugin") version "3.2.2"
    id("java")
}

// Aplicar plugin simple para tests
apply(from = "gradle/simple-test.gradle")

group = "com.cadetex"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.cadetex.ApplicationKt")
}

repositories {
    mavenCentral()
    maven("https://repo.maven.apache.org/maven2")
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:3.2.2")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.2")
    implementation("io.ktor:ktor-server-cio-jvm:3.2.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.2.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.2.2")
    implementation("io.ktor:ktor-server-cors-jvm:3.2.2")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.2.2")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.2.2")
    implementation("io.ktor:ktor-server-auth-jvm:3.2.2")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.2.2")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.56.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.56.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.56.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.56.0")
    implementation("com.h2database:h2:2.2.224")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Security
    implementation("org.mindrot:jbcrypt:0.4")

    // OpenAPI/Swagger
    implementation("io.ktor:ktor-server-openapi:3.2.2")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:3.2.2")
    testImplementation("io.ktor:ktor-client-content-negotiation:3.2.2")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:3.2.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}