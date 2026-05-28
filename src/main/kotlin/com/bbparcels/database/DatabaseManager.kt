package com.bbparcels.database

import com.bbparcels.BbParcels
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

class DatabaseManager(private val plugin: BbParcels) {

    private var dataSource: HikariDataSource? = null

    fun connect() {
        val cfg = plugin.config

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:mariadb://${cfg.getString("database.host")}:${cfg.getInt("database.port")}/${cfg.getString("database.name")}"
            username = cfg.getString("database.username")
            password = cfg.getString("database.password")
            driverClassName = "org.mariadb.jdbc.Driver"
            maximumPoolSize = cfg.getInt("database.pool-size", 10)
            minimumIdle = 2
            connectionTimeout = 30_000
            idleTimeout = 600_000
            maxLifetime = 1_800_000
        }

        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = plugin.javaClass.classLoader
        try {
            dataSource = HikariDataSource(hikariConfig)
        } finally {
            Thread.currentThread().contextClassLoader = previous
        }

        createTables()
        plugin.logger.info("Database connected successfully!")
    }

    fun disconnect() {
        dataSource?.close()
        plugin.logger.info("Database disconnected.")
    }
    

    fun getConnection(): Connection = dataSource!!.connection
    
    private fun createTables() {
        val sql = plugin.getResource("schema.sql")
            ?.bufferedReader()
            ?.readText()
            ?: return

        getConnection().use { conn ->
            // Exécute chaque statement séparé par ";"
            sql.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { statement ->
                    conn.createStatement().use { it.execute(statement) }
                }
        }
    }
}
