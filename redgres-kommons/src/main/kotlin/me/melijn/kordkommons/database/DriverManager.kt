package me.melijn.kordkommons.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.CompressionCodec
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.kordkommons.async.TaskScope
import me.melijn.kordkommons.redis.RedisConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

public class DriverManager(
    hikariConfig: HikariConfig,
    redisConfig: RedisConfig,
    private val runAfterConnect: (Transaction.() -> Unit)? = null
) {

    private val afterConnectToBeExecutedQueries = ArrayList<String>()

    private val logger = LoggerFactory.getLogger(DriverManager::class.java.name)
    private val postgresqlPattern = "(\\d+\\.\\d+).*".toRegex()
    public val database: Database
    private val dataSource: DataSource

    public var redisClient: RedisClient? = null
    public var redisConnection: StatefulRedisConnection<String, String?>? = null
    public var compressionRedisConnection: StatefulRedisConnection<String, String?>? = null

    init {
        // HIKARI

        dataSource = HikariDataSource(hikariConfig)
        database = Database.connect(dataSource)

        logger.info("Creating tables...")
        transaction {
            runAfterConnect?.let { it() }
        }
        logger.info("Created tables")

        if (redisConfig.enabled) {
            logger.info("Connecting to redis..")
            connectRedis(redisConfig)
        }
    }

    private fun connectRedis(redisConfig: RedisConfig) {
        var uriBuilder = RedisURI.builder()
            .withHost(redisConfig.host)
            .withPort(redisConfig.port)

        if (redisConfig.password.isNotBlank()) {
            uriBuilder = if (redisConfig.user?.isBlank() == false) {
                uriBuilder.withAuthentication(redisConfig.user, redisConfig.password)
            } else {
                uriBuilder.withPassword(redisConfig.password.toCharArray())
            }
        }

        val uri = uriBuilder.build()

        val redisClient = RedisClient
            .create(uri)

        this.redisClient = redisClient

        try {
            redisConnection = redisClient.connect()
            compressionRedisConnection = redisClient?.connect(
                CompressionCodec.valueCompressor(StringCodec.UTF8, CompressionCodec.CompressionType.GZIP)
            )
            logger.info("Connected to redis")

        } catch (e: Throwable) {
            TaskScope.launch {
                logger.warn("Retrying to connect to redis..")
                recursiveConnectRedis(redisConfig.host, redisConfig.port)
                logger.warn("Retrying to connect to redis has succeeded!")
            }
        }
    }

    private suspend fun recursiveConnectRedis(host: String, port: Int) {
        try {
            redisConnection = redisClient?.connect()
            compressionRedisConnection = redisClient?.connect(
                CompressionCodec.valueCompressor(StringCodec.UTF8, CompressionCodec.CompressionType.GZIP)
            )
        } catch (e: Throwable) {
            delay(5_000)
            recursiveConnectRedis(host, port)
        }
    }

    public fun getUsableConnection(function: (Connection) -> Unit) {
        val startConnection = System.currentTimeMillis()
        dataSource.connection.use {
            function(it)
        }
        if (System.currentTimeMillis() - startConnection > 3_000)
            logger.info("Connection collected: Alive for ${(System.currentTimeMillis() - startConnection)}ms")
    }

    public suspend fun getDBVersion(): String = suspendCoroutine {
        try {
            getUsableConnection { con ->
                it.resume(
                    con.metaData.databaseProductVersion.replace(postgresqlPattern, "$1")
                )
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            it.resume("error")
        }
    }

    public suspend fun getConnectorVersion(): String = suspendCoroutine {
        try {
            getUsableConnection { con ->
                it.resume(con.metaData.driverVersion)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            it.resume("error")
        }
    }

    public fun registerTable(table: String, tableStructure: String, primaryKey: String, uniqueKey: String = "") {
        val hasPrimary = primaryKey != ""
        val hasUnique = uniqueKey != ""
        afterConnectToBeExecutedQueries.add(
            "CREATE TABLE IF NOT EXISTS $table ($tableStructure${
                if (hasPrimary) {
                    ", PRIMARY KEY ($primaryKey)"
                } else {
                    ""
                }
            }${if (hasUnique) ", UNIQUE ($uniqueKey)" else ""})"
        )
    }

    public fun executeTableRegistration() {
        getUsableConnection { connection ->
            connection.createStatement().use { statement ->
                afterConnectToBeExecutedQueries.forEach { tableRegistrationQuery ->
                    statement.addBatch(tableRegistrationQuery)
                }
                statement.executeBatch()
            }
        }
    }

    /**
     * Runs a modifying query on the database.
     *
     * **Example Usage**
     *```
     * val modifiedRows = driverManager.executeUpdateGetChanged(
     *     "UPDATE apples SET bad = ? WHERE id = ?",
     *     true, 6
     * )
     * println(modifiedRows) // > 1
     * ```
     * @param query the sql query that needs execution
     * @param objects the arguments of the query
     *
     * @return the amount of modified rows
     */
    public suspend fun executeUpdateGetChanged(query: String, vararg objects: Any?): Int = suspendCoroutine {
        try {
            getUsableConnection { connection ->
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    val rows = preparedStatement.executeUpdate()
                    it.resume(rows)
                }
            }
        } catch (e: SQLException) {
            logger.error("Something went wrong when executing the query: $query\nObjects: ${objects.joinToString { o -> o.toString() }}")
            e.printStackTrace()
        }
    }

    /**
     * Runs a modifying query on the database.
     *
     * **Example Usage**
     *```
     * riverManager.executeUpdate(
     *     "UPDATE apples SET bad = ? WHERE id = ?",
     *     true, 6
     * )
     * ```
     * @param query the sql query that needs execution
     * @param objects the arguments of the query
     *
     */
    public fun executeUpdate(query: String, vararg objects: Any?) {
        try {
            getUsableConnection { connection ->
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    preparedStatement.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            logger.error("Something went wrong when executing the query: $query\nObjects: ${objects.joinToString { o -> o.toString() }}")
            e.printStackTrace()
        }
    }

    /**
     * Vararg wrapper function for [executeQueryList]
     */
    public fun executeQuery(query: String, resultset: (ResultSet) -> Unit, vararg objects: Any?) {
        executeQueryList(query, resultset, objects.toList())
    }

    /**
     * Runs a selecting query on the database.
     *
     *
     * **Example Usage**
     *```
     * val updatedRows = driverManager.executeQueryList(
     *     "SELECT * FROM apples WHERE id = ?",
     *     { rs -> println(rs.getBoolean("bad")) },
     *     5
     * )
     * println("hi")
     * // > hi
     * // > true
     * ```
     * @param query the sql query that needs execution
     * @param objects the arguments of the query
     *
     * @return the potentially generated key
     */
    public fun executeQueryList(query: String, callback: (ResultSet) -> Unit, objects: List<Any?>) {
        try {
            getUsableConnection { connection ->
                if (connection.isClosed) {
                    logger.warn("Connection closed: $query")
                }
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    preparedStatement.executeQuery().use { resultSet -> callback(resultSet) }
                }
            }
        } catch (e: SQLException) {
            logger.error(
                "Something went wrong when executing the query: $query\n" +
                    "Objects: ${objects.joinToString { o -> o.toString() }}", e
            )
        }
    }

    /**
     * Runs a modifying query on the database.
     *
     * **Example Usage**
     *```
     * val newId = driverManager.executeUpdateGetGeneratedKeys(
     *     "INSERT INTO apples (bad) VALUES (?)",
     *     true
     * )
     * println(newId) // > 6
     * ```
     * @param query the sql query that needs execution
     * @param objects the arguments of the query
     *
     * @throws IllegalArgumentException if the query did not generate a key, this can happen when there is no AUTO_INCREMENT on a column
     *
     * @return the potentially generated key
     */
    public suspend fun executeUpdateGetGeneratedKeys(query: String, vararg objects: Any?): Long = suspendCoroutine {
        try {
            getUsableConnection { connection ->
                connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }

                    preparedStatement.executeUpdate()
                    preparedStatement.generatedKeys.use { rs ->
                        if (rs.next()) {
                            it.resume(rs.getLong(2))
                        } else {
                            throw IllegalArgumentException("No keys were generated when executing: $query")
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(
                "Something went wrong when executing the query: $query\n" +
                    "Objects: ${objects.joinToString { o -> o.toString() }}"
            )
            e.printStackTrace()
        }
    }

    /**
     * Clears all the entries from [table]
     */
    public fun clear(table: String): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement("TRUNCATE $table").use { preparedStatement ->
                return preparedStatement.executeUpdate()
            }
        }
    }

    /**
     * Deletes the [table] and all it's entries
     */
    public fun dropTable(table: String) {
        afterConnectToBeExecutedQueries.add(0, "DROP TABLE $table")
    }

    public fun getOpenRedisConnection(compress: Boolean = false): RedisAsyncCommands<String, String?>? {
        if (compress){
            if (compressionRedisConnection?.async()?.isOpen == true) {
                return compressionRedisConnection?.async()
            }
        } else {
            if (redisConnection?.async()?.isOpen == true) {
                return redisConnection?.async()
            }
        }
        return null
    }

    /**
     * Tells redis to store [key] -> [value] with an expiry duration of [ttl] [ttlUnit].
     *
     * @param key the key
     * @param value the value
     * @param ttl The time to live for this entry, also see [ttlUnit]
     * @param ttlUnit [TimeUnit.MINUTES] by default
     * @param compress Whether redis should compress the sent data when storing it in ram
     */
    public fun setCacheEntry(key: String, value: String, ttl: Int? = null, ttlUnit: TimeUnit = TimeUnit.MINUTES, compress: Boolean = false) {
        val async = getOpenRedisConnection(compress) ?: return
        if (ttl == null) async.set(key, value)
        else {
            val ttlSeconds = TimeUnit.SECONDS.convert(ttl.toLong(), ttlUnit)
            async.set(key, value, SetArgs().ex(ttlSeconds))
        }
    }

    /**
     * Tells redis to store [key] -> [value] with your own provided arguments
     *
     * @param key the key
     * @param value the value
     * @param args Optional lettuce [SetArgs] object.
     * @param compress Whether redis should compress the sent data when storing it in ram
     */
    public fun setCacheEntryWithArgs(key: String, value: String, args: SetArgs? = null, compress: Boolean = false) {
        val async = getOpenRedisConnection(compress) ?: return
        if (args == null) async.set(key, value)
        else async.set(key, value, args)
    }

    /**
     * Tells redis to fetch [key]'s value with a cache refresh duration of [ttl] [ttlUnit].
     *
     * @param key the key
     * @param ttl the new time to live for this entry, also see [ttlUnit], when null the entry will keep it's previous ttl.
     * @param ttlUnit [TimeUnit.MINUTES] by default
     * @param compress Whether redis should compress the sent data when storing it in ram
     *
     * @return the associated value of [key], can be `null` if missing
     */
    public suspend fun getCacheEntry(key: String, ttl: Int? = null, ttlUnit: TimeUnit = TimeUnit.MINUTES, compress: Boolean = false): String? {
        val commands = getOpenRedisConnection(compress) ?: return null
        val result = commands
            .get(key)
            .await()
        if (result != null && ttl != null) {
            val ttlSeconds = TimeUnit.SECONDS.convert(ttl.toLong(), ttlUnit)
            commands.expire(key, ttlSeconds)
        }
        return result
    }

    /**
     * Bloody obvious innit
     */
    public fun removeCacheEntry(key: String) {
        val con = getOpenRedisConnection() ?: return
        con.del(key)
    }
}