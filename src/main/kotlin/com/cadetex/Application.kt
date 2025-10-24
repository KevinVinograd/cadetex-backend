package com.cadetex

import com.cadetex.auth.configureAuthentication
import com.cadetex.validation.configureValidation
import io.ktor.server.application.*
import io.ktor.server.cio.EngineMain
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    EngineMain.main(emptyArray())
}

fun Application.module() {
    logger.info("=== CADETEX BACKEND STARTING ===")
    logger.info("Application version: 1.0.0")
    logger.info("Ktor version: ${io.ktor.server.engine.ApplicationEngine::class.java.`package`.implementationVersion ?: "Unknown"}")
    logger.info("Java version: ${System.getProperty("java.version")}")
    logger.info("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
    
    configureSerialization()
    logger.info("✓ Serialization configured")
    
    configureDatabases()
    logger.info("✓ Database connection configured")
    
    configureCORS()
    logger.info("✓ CORS configured")
    
    configureLogging()
    logger.info("✓ Logging configured")
    
    configureValidation()
    logger.info("✓ Validation configured")
    
    configureAuthentication()
    logger.info("✓ Authentication configured")
    
    configureOpenAPI()
    logger.info("✓ OpenAPI documentation configured")
    
    configureRouting()
    logger.info("✓ Routes configured")
    
    logger.info("=== CADETEX BACKEND READY ===")
}
