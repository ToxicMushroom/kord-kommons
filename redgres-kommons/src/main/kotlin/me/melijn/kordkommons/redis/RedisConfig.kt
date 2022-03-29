package me.melijn.kordkommons.redis

data class RedisConfig(
    // whether you want to connect to redis at all
    val enabled: Boolean,

    // eg. localhost
    val host: String,

    // 6379
    val port: Int,

    // nullable user, can be left null
    val user: String?,

    // password is required for minimal security
    val password: String
)