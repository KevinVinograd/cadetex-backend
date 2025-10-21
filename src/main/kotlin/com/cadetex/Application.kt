package com.cadetex

import com.cadetex.auth.configureAuthentication
import com.cadetex.validation.configureValidation
import io.ktor.server.application.*

fun main() {
    // Application will be started by Ktor plugin
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureCORS()
    configureLogging()
    configureValidation()
    configureAuthentication()
    configureRouting()
}
