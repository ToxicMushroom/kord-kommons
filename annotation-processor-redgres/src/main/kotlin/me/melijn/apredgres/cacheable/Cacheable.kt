package me.melijn.apredgres.cacheable

/**
 * TTL in seconds
 */
public annotation class Cacheable(
    val ttl: Int = 300,
    val refreshCacheOnFetch: Boolean = true
)
