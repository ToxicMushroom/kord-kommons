package me.melijn.kordkommons.database

import com.zaxxer.hikari.HikariConfig

public object ConfigUtil {

    /**
     * An HikariConfig with jdbc connection to your [host] via the postgresql connector.
     * And with following configuration values:
     * @post | return.maxLifetime == 30_000 (30s)
     * @post | return.validationTimeout == 3_000 (3s)
     * @post | return.connectionTimeout == 30_000 (30s)
     * @post | return.leakDetectionThreshold == 2000 (2s)
     * @post | return.maximumPoolSize == 10
     *
     */
    public fun generateDefaultHikariConfig(
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