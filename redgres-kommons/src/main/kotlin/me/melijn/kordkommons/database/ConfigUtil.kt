package me.melijn.kordkommons.database

import com.zaxxer.hikari.HikariConfig

object ConfigUtil {

    fun generateDefaultHikariConfig(
        host: String, port: Int, db: String, user: String, password: String
    ): HikariConfig {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:postgresql://${host}:${port}/${db}"
        config.username = user
        config.password = password

        config.maxLifetime = 30_000
        config.validationTimeout = 3_000
        config.connectionTimeout = 30_000
        config.leakDetectionThreshold = 2000
        config.maximumPoolSize = 10

        return config
    }
}