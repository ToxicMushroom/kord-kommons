package me.melijn.apkordtesting.database.model

import me.melijn.apredgres.cacheable.Cacheable
import me.melijn.apredgres.createtable.CreateTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

@CreateTable
@Cacheable
object TestModel : Table("stuff") {

    val id = uuid("id")
    val name = text("name").nullable()
    val added = datetime("added")
    val birthday = date("birthday")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, name)
        index(false, birthday)
    }
}