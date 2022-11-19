package me.melijn.kordkommons.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction

public open class DBTableManager<T : Table>(
    public open val driverManager: DriverManager,
    public val table: Table
) {

    public inline fun <L> scopedTransaction(crossinline func: (Transaction) -> L): L = transaction(driverManager.database) {
        func(this)
    }

    public fun <K> newOrUpdate(
        insert: T.(InsertStatement<Number>) -> Unit, update: T.(UpdateBuilder<Int>) -> Unit,
        results: InsertOrUpdate<Number>.() -> K
    ): K = scopedTransaction {
        (table as T).insertOrUpdate(insert, update, results)
    }

    public fun <K> newOrUpdate(
        insert: T.(InsertStatement<Number>) -> Unit, update: T.(UpdateBuilder<Int>) -> Unit
    ): Int? = scopedTransaction {
        (table as T).insertOrUpdate(insert, update)
    }
}