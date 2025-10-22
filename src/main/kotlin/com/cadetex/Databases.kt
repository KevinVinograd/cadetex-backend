package com.cadetex

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.cadetex.database.tables.*

fun Application.configureDatabases() {
    val config = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = "jdbc:postgresql://localhost:5432/cadetex"
        username = "cadetex_user"
        password = "cadetex_password"
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    
    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)

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
