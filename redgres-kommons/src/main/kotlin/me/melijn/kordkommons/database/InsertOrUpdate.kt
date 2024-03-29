package me.melijn.kordkommons.database

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.transactions.TransactionManager

/**
 * Insert or update a-ka upsert implementation
 *
 * Sample usage:
 *
 * class SampleTable: IntIdTable("whatever"){
 *     val identifier = varchar("identifier", 32").uniqueIndex()
 *     val value = varchar("value", 32)
 * }
 *
 * transaction {
 *     SampleTable.insertOrUpdate({
 *         it[SampleTable.identifier] = "some identifier"
 *         it[SampleTable.value] = "inserted"
 *     }){
 *         it[SampleTable.value] = "updated"
 *     }
 * }
 *
 * Which is equivalent of:
 *
 * INSERT INTO whatever(identifier, value) VALUES('some identifier', 'inserted')
 * ON DUPLICATE KEY UPDATE value = 'updated'
 */
public fun <T : Table, K> T.insertOrUpdate(
    insert: T.(InsertStatement<Number>) -> Unit,
    update: T.(UpdateBuilder<Int>) -> Unit,
    results: InsertOrUpdate<Number>.() -> K
): K {
    val updateQuery = UpsertUpdateBuilder(this).apply {
        update(this)
    }
    return InsertOrUpdate<Number>(updateQuery, this).run {
        insert(this)
        val res = results()
        execute(TransactionManager.current())
        res
    }
}

public fun <T : Table> T.insertOrUpdate(
    insert: T.(InsertStatement<Number>) -> Unit,
    update: T.(UpdateBuilder<Int>) -> Unit
): Int? {
    val updateQuery = UpsertUpdateBuilder(this).apply {
        update(this)
    }
    return InsertOrUpdate<Number>(updateQuery, this).run {
        insert(this)
        execute(TransactionManager.current())
    }
}

public class UpsertUpdateBuilder(table: Table) : UpdateBuilder<Int>(StatementType.OTHER, listOf(table)) {

    public val firstDataSet: List<Pair<Column<*>, Any?>> get() = values.toList()

    override fun arguments(): List<List<Pair<IColumnType, Any?>>> = QueryBuilder(true).run {
        values.forEach {
            registerArgument(it.key, it.value)
        }
        if (args.isNotEmpty()) listOf(args) else emptyList()
    }

    override fun prepareSQL(transaction: Transaction): String {
        throw IllegalStateException("prepareSQL in UpsertUpdateBuilder is not supposed to be used")
    }

    override fun PreparedStatementApi.executeInternal(transaction: Transaction): Int {
        throw IllegalStateException("executeInternal in UpsertUpdateBuilder is not supposed to be used")
    }
}

public class InsertOrUpdate<Key : Any>(
    public val update: UpsertUpdateBuilder, table: Table, isIgnore: Boolean = false
) : InsertStatement<Key>(table, isIgnore) {

    override fun arguments(): List<List<Pair<IColumnType, Any?>>> {
        val updateArgs = update.arguments()
        return super.arguments().mapIndexed { index, list ->
            list + (updateArgs.getOrNull(index) ?: return@mapIndexed list.toList())
        }
    }

    override fun prepareSQL(transaction: Transaction): String {
        val values = update.firstDataSet
        if (values.isEmpty()) return super.prepareSQL(transaction)


        val originalStatement = super.prepareSQL(transaction)

        val updateStm = with(QueryBuilder(true)) {
            values.appendTo(this) { (col, value) ->
                append("${transaction.identity(col)}=")
                registerArgument(col, value)
            }
            toString()
        }

        return "$originalStatement ON CONFLICT(${table.primaryKey?.columns?.joinToString { it.name } ?: (table as IdTable<*>).id.name}) DO UPDATE SET $updateStm"
    }
}