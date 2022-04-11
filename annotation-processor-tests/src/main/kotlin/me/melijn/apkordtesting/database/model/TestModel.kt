package me.melijn.apkordtesting.database.model

import me.melijn.ap.cacheable.Cacheable
import me.melijn.ap.createtable.CreateTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

@CreateTable
@Cacheable
object TestModel : Table("stuff") {

    val id = uuid("id")
    val name = text("name")
    val added = datetime("added")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, name)
    }
}