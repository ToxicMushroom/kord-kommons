package me.melijn.kordkommons.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import me.melijn.kordkommons.async.TaskManager
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

class DriverManager(
    hikariConfig: HikariConfig,
    redisConfig: RedisConfig,
    private val runAfterConnect: (Transaction.() -> Unit)? = null
) {

    private val afterConnectToBeExecutedQueries = ArrayList<String>()

    private val logger = LoggerFactory.getLogger(DriverManager::class.java.name)
    private val postgresqlPattern = "(\\d+\\.\\d+).*".toRegex()
    val database: Database
    private val dataSource: DataSource

    var redisClient: RedisClient? = null
    var redisConnection: StatefulRedisConnection<String, String?>? = null

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
            connectRedis(redisConfig.host, redisConfig.port, redisConfig.user, redisConfig.password)
        }
    }

    private fun connectRedis(host: String, port: Int, user: String?, password: String) {
        var uriBuilder = RedisURI.builder()
            .withHost(host)
            .withPort(port)

        if (password.isNotBlank()) {
            uriBuilder = if (user?.isBlank() == false) {
                uriBuilder.withAuthentication(user, password)
            } else {
                uriBuilder.withPassword(password.toCharArray())
            }
        }

        val uri = uriBuilder.build()

        val redisClient = RedisClient
            .create(uri)

        this.redisClient = redisClient

        try {
            redisConnection = redisClient.connect()
            logger.info("Connected to redis")

        } catch (e: Throwable) {
            TaskManager.async {
                logger.warn("Retrying to connect to redis..")
                recursiveConnectRedis(host, port)
                logger.warn("Retrying to connect to redis has succeeded!")
            }
        }
    }

    private suspend fun recursiveConnectRedis(host: String, port: Int) {
        try {
            redisConnection = redisClient?.connect()
        } catch (e: Throwable) {
            delay(5_000)
            recursiveConnectRedis(host, port)
        }
    }

    fun getUsableConnection(function: (Connection) -> Unit) {
        val startConnection = System.currentTimeMillis()
        dataSource.connection.use {
            function(it)
        }
        if (System.currentTimeMillis() - startConnection > 3_000)
            logger.info("Connection collected: Alive for ${(System.currentTimeMillis() - startConnection)}ms")
    }

    fun registerTable(table: String, tableStructure: String, primaryKey: String, uniqueKey: String = "") {
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

    fun executeTableRegistration() {
        getUsableConnection { connection ->
            connection.createStatement().use { statement ->
                afterConnectToBeExecutedQueries.forEach { tableRegistrationQuery ->
                    statement.addBatch(tableRegistrationQuery)
                }
                statement.executeBatch()
            }
        }
    }

    /** returns the amount of rows affected by the query
     * [query] the sql query that needs execution
     * [objects] the arguments of the query
     * [Int] returns the amount of affected rows
     * example:
     *   query: "UPDATE apples SET bad = ? WHERE id = ?"
     *   objects: true, 6
     *   return value: 1
     * **/
    suspend fun executeUpdateGetChanged(query: String, vararg objects: Any?): Int = suspendCoroutine {
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

    /** returns the amount of rows affected by the query
     * [query] the sql query that needs execution
     * [objects] the arguments of the query
     * [Int] returns the amount of affected rows
     * example:
     *   query: "UPDATE apples SET bad = ? WHERE id = ?"
     *   objects: true, 6
     *   return value: 1
     * **/
    fun executeUpdate(query: String, vararg objects: Any?) {
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
     * [query] the sql query that needs execution
     * [resultset] The consumer that will contain the resultset after executing the query
     * [objects] the arguments of the query
     * example:
     *   query: "SELECT * FROM apples WHERE id = ?"
     *   objects: 5
     *   resultset: Consumer object to handle the resultset
     * **/
    fun executeQuery(query: String, resultset: (ResultSet) -> Unit, vararg objects: Any?) {
        executeQueryList(query, resultset, objects.toList())
    }

    /**
     * [query] the sql query that needs execution
     * [resultset] The consumer that will contain the resultset after executing the query
     * [objects] the arguments of the query
     * example:
     *   query: "SELECT * FROM apples WHERE id = ?"
     *   objects: 5
     *   resultset: Consumer object to handle the resultset
     * **/
    fun executeQueryList(query: String, resultset: (ResultSet) -> Unit, objects: List<Any?>) {
        try {
            getUsableConnection { connection ->
                if (connection.isClosed) {
                    logger.warn("Connection closed: $query")
                }
                connection.prepareStatement(query).use { preparedStatement ->
                    for ((index, value) in objects.withIndex()) {
                        preparedStatement.setObject(index + 1, value)
                    }
                    preparedStatement.executeQuery().use { resultSet -> resultset.invoke(resultSet) }
                }
            }
        } catch (e: SQLException) {
            logger.error(
                "Something went wrong when executing the query: $query\n" +
                    "Objects: ${objects.joinToString { o -> o.toString() }}", e
            )
        }
    }

    suspend fun getDBVersion(): String = suspendCoroutine {
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

    suspend fun getConnectorVersion(): String = suspendCoroutine {
        try {
            getUsableConnection { con ->
                it.resume(
                    con.metaData.driverVersion
                )
            }

        } catch (e: SQLException) {
            e.printStackTrace()
            it.resume("error")
        }
    }

    fun clear(table: String): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement("TRUNCATE $table").use { preparedStatement ->
                return preparedStatement.executeUpdate()
            }
        }
    }

    /** returns the amount of rows affected by the query
     * [query] the sql query that needs execution
     * [objects] the arguments of the query
     * example:
     *   query: "UPDATE apples SET bad = ? WHERE id = ?"
     *   objects: true, 6
     *   return value: 1
     * **/
    suspend fun executeUpdateGetGeneratedKeys(query: String, vararg objects: Any?): Long = suspendCoroutine {
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

    fun dropTable(table: String) {
        afterConnectToBeExecutedQueries.add(0, "DROP TABLE $table")
    }

    fun getOpenRedisConnection(): RedisAsyncCommands<String, String?>? {
        if (redisConnection?.async()?.isOpen == true) {
            return redisConnection?.async()
        }
        return null
    }

    // ttl: minutes
    fun setCacheEntry(key: String, value: String, ttl: Int? = null, ttlUnit: TimeUnit = TimeUnit.MINUTES) {
        val async = getOpenRedisConnection() ?: return
        if (ttl == null) async.set(key, value)
        else {
            val ttlSeconds = TimeUnit.SECONDS.convert(ttl.toLong(), ttlUnit)
            async.set(key, value, SetArgs().ex(ttlSeconds))
        }
    }

    fun setCacheEntryWithArgs(key: String, value: String, args: SetArgs? = null) {
        val async = getOpenRedisConnection() ?: return
        if (args == null) async.set(key, value)
        else async.set(key, value, args)
    }

    // ttl: minutes
    suspend fun getCacheEntry(key: String, ttlMinutes: Int? = null): String? {
        val commands = getOpenRedisConnection() ?: return null
        val result = commands
            .get(key)
            .await()
        if (result != null && ttlMinutes != null) {
            commands.expire(key, ttlMinutes * 60L)
        }
        return result
    }

    fun removeCacheEntry(key: String) {
        val con = getOpenRedisConnection() ?: return
        con.del(key)
    }
}