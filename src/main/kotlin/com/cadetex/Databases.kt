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
    
    // Preferir variables de entorno/propiedades de sistema para tests (Testcontainers)
    val env = System.getenv()
    val sys = System.getProperties()

    // application.conf
    val appCfg = environment.config
    val jdbcFromConfig = appCfg.propertyOrNull("database.jdbcUrl")?.getString()
    val host = appCfg.propertyOrNull("database.host")?.getString() ?: "localhost"
    val port = appCfg.propertyOrNull("database.port")?.getString()?.toIntOrNull() ?: 5432
    val name = appCfg.propertyOrNull("database.name")?.getString() ?: "kdt"
    val userConf = appCfg.propertyOrNull("database.user")?.getString() ?: "cadetex_user"
    val passConf = appCfg.propertyOrNull("database.password")?.getString() ?: "cadetex_password"
    val poolSizeConf = appCfg.propertyOrNull("database.maxPoolSize")?.getString()?.toIntOrNull()

    // Overrides por props/env (System properties PRIORIDAD para que Testcontainers de tests gane sobre env del host)
    val dbUrlOverride = (sys.getProperty("DB_URL") ?: env["DB_URL"])
    val dbUserOverride = (sys.getProperty("DB_USER") ?: env["DB_USER"])
    val dbPassOverride = (sys.getProperty("DB_PASSWORD") ?: env["DB_PASSWORD"])
    val poolOverride = (sys.getProperty("DB_MAX_POOL") ?: env["DB_MAX_POOL"])?.toIntOrNull()

    val resolvedJdbcUrl = dbUrlOverride ?: jdbcFromConfig ?: "jdbc:postgresql://$host:$port/$name"
    val resolvedUser = dbUserOverride ?: userConf
    val resolvedPass = dbPassOverride ?: passConf
    val resolvedPool = poolOverride ?: poolSizeConf ?: 10

    val config = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = resolvedJdbcUrl
        username = resolvedUser
        password = resolvedPass
        maximumPoolSize = resolvedPool
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
            Addresses,
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
