package me.melijn.kordkommons.environment

import io.github.cdimascio.dotenv.dotenv
import kotlin.properties.ReadOnlyProperty

/**
 * group will be used to determine the ENV base
 * ex. `group = "bot.info"`
 * and we have a field:
 * `val token: String by env("token")`
 * then the resolving ENV var is "BOT_INFO_TOKEN"
 *
 * dots will be replaced by underscores
 *
 * If you need more delegate types, write extension functions or consider a PR
 */
open class BotSettings(val group: String) {
    private val transformedGroup = group.replace(".", "_").uppercase()

    companion object {
        var dotEnv = dotenv {
            this.filename = System.getenv("ENV_FILE") ?: ".env"
            this.ignoreIfMissing = true
        }
    }

    fun string(key: String, default: String? = null) = ReadOnlyProperty<BotSettings, String> { _, _ ->
        getValue(key, default) { t -> t }
    }

    fun stringN(key: String) = ReadOnlyProperty<BotSettings, String?> { _, _ ->
        getStringValue(key)
    }

    fun long(key: String, default: Long? = null) = ReadOnlyProperty<BotSettings, Long> { _, _ ->
        getValue(key, default) { t -> t.toLong() }
    }

    fun int(key: String, default: Int? = null) = ReadOnlyProperty<BotSettings, Int> { _, _ ->
        getValue(key, default) { t -> t.toInt() }
    }

    fun boolean(key: String, default: Boolean? = null) = ReadOnlyProperty<BotSettings, Boolean> { _, _ ->
        getValue(key, default) { t -> t.toBoolean() }
    }

    fun float(key: String, default: Float? = null) = ReadOnlyProperty<BotSettings, Float> { _, _ ->
        getValue(key, default) { t -> t.toFloat() }
    }


    fun <T> getValue(key: String, default: T?, convertor: (String) -> T): T {
        val value = getStringValueN(key, default)
        return if (value != null) convertor(value)
        else default ?: throw IllegalStateException()
    }

    fun getStringValue(key: String) = getStringValueN(key, null) ?: throw IllegalStateException()
    fun getStringValueN(key: String, default: Any?): String? {
        val value = getStringValueN(key)
        if (value == null && default == null) throw IllegalStateException("missing env value for key: $key")
        return value
    }


    fun getStringValueN(key: String) = dotEnv[transformedGroup + "_" + key.uppercase()]
}