package me.melijn.apredgres.tablemodel

/**
 * TTL in seconds
 */
public annotation class TableModel(
    val cacheable: Boolean = true,
    val ttl: Int = 300,
    val refreshCacheOnFetch: Boolean = true
)
