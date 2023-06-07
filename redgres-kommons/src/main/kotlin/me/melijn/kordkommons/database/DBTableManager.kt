package me.melijn.kordkommons.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.sql.Connection

public open class DBTableManager<T : Table>(
    public open val driverManager: DriverManager,
    public val table: Table
) {

    public suspend inline fun <L> scopedTransaction(
        transactionIsolation: Int = Connection.TRANSACTION_REPEATABLE_READ,
        crossinline func: (Transaction) -> L
    ): L {
        return newSuspendedTransaction(transactionIsolation = transactionIsolation, db = driverManager.database) {
            func(this)
        }
    }

    public suspend fun <K> newOrUpdate(
        insert: T.(InsertStatement<Number>) -> Unit, update: T.(UpdateBuilder<Int>) -> Unit,
        results: InsertOrUpdate<Number>.() -> K
    ): K = scopedTransaction {
        (table as T).insertOrUpdate(insert, update, results)
    }

    public suspend fun <K> newOrUpdate(
        insert: T.(InsertStatement<Number>) -> Unit, update: T.(UpdateBuilder<Int>) -> Unit
    ): Int? = scopedTransaction {
        (table as T).insertOrUpdate(insert, update)
    }
}