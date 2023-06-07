package me.melijn.kordkommons.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.util.IsolationLevel

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
     * @post | return.isAutoCommit == false
     * @post | return.transactionIsolation == "TRANSACTION_REPEATABLE_READ"
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

        config.poolName = "RedgresPool"
        config.maximumPoolSize = 10

        // https://stackoverflow.com/a/41206003/7271796
        config.isAutoCommit = false

        // https://github.com/JetBrains/Exposed/wiki/DSL#batch-insert
        config.addDataSourceProperty("reWriteBatchedInserts", "true")

        // Avoids race conditions between transactions that are modifying the same rows
        config.transactionIsolation = IsolationLevel.TRANSACTION_REPEATABLE_READ.name
        return config
    }
}