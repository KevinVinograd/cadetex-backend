package com.cadetex

import com.cadetex.auth.configureAuthentication
import com.cadetex.validation.configureValidation
import io.ktor.server.application.*
import io.ktor.server.cio.EngineMain

fun main() {
    EngineMain.main(emptyArray())
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureCORS()
    configureLogging()
    configureValidation()
    configureAuthentication()
    configureOpenAPI()
    configureRouting()
}
