package com.cadetex

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.cadetex.database.tables.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Database")

fun Application.configureDatabases() {
    logger.info("Configuring database connection...")
    
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
    
    logger.info("Database configuration:")
    logger.info("  - Driver: ${config.driverClassName}")
    logger.info("  - URL: ${config.jdbcUrl}")
    logger.info("  - Username: ${config.username}")
    logger.info("  - Max Pool Size: ${config.maximumPoolSize}")
    logger.info("  - Auto Commit: ${config.isAutoCommit}")
    logger.info("  - Transaction Isolation: ${config.transactionIsolation}")
    
    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)
    logger.info("✓ Database connection established")

    // Crear todas las tablas automáticamente
    logger.info("Creating database tables...")
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
    logger.info("✓ Database tables created/verified")
}
