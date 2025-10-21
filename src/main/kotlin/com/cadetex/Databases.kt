package com.cadetex

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.cadetex.database.tables.*

fun Application.configureDatabases() {
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;")
    
    // Crear todas las tablas automáticamente
    transaction {
        SchemaUtils.create(
            Organizations,
            Users,
            Clients,
            Providers,
            Couriers,
            Tasks,
            TaskPhotos,
            TaskHistory
        )
    }
}
