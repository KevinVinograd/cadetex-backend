package com.cadetex

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Logging")

fun Application.configureLogging() {
    logger.info("Configuring application logging...")
    
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
        }
    }
    
    logger.info("Logging configuration:")
    logger.info("  - Call logging level: ${Level.INFO}")
    logger.info("  - Log files: logs/application.log, logs/errors.log")
    logger.info("  - Console output: enabled")
    logger.info("✓ Logging configured successfully")
}

