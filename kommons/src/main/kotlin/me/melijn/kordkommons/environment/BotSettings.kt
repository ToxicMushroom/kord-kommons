package me.melijn.kordkommons.environment

import io.github.cdimascio.dotenv.dotenv
import me.melijn.kordkommons.utils.camelToSnake
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
    var splitOnCammelCase = globalSplitOnCammelCase

    companion object {
        var dotEnv = dotenv {
            this.filename = System.getenv("ENV_FILE") ?: ".env"
            this.ignoreIfMissing = true
        }
        var globalSplitOnCammelCase = false
    }

    fun stringList(key: String, default: List<String> = emptyList()) = ReadOnlyProperty<BotSettings, List<String>> { _, _ ->
        var i = 0
        val list = mutableListOf<String>()
        var value = getStringValueN(key + i++)?.also { list.add(it) }
        while (value != null) {
            value = getStringValueN(key + i++)?.also { list.add(it) }
        }
        list.takeIf { it.isNotEmpty() } ?: default
    }

    fun string(key: String, default: String? = null) = ReadOnlyProperty<BotSettings, String> { _, _ ->
        getValue(key, default) { t -> t }
    }

    fun stringN(key: String) = ReadOnlyProperty<BotSettings, String?> { _, _ ->
        getStringValueN(key)
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

    inline fun <reified T : Enum<T>> enum(key: String, default: String? = null) =
        ReadOnlyProperty<BotSettings, T> { _, _ ->
            val value = getValue(key, default) { t -> t }
            val enumValues = enumValues<T>()
            enumValues.firstOrNull { value.equals(it.toString(), true) }
                ?: throw IllegalArgumentException(
                    "the env entry: ${group}_${key}=${value} has an incorrect value," +
                        " the value should be one of the following: " +
                        enumValues.joinToString()
                )
        }

    inline fun <reified T : Enum<T>> enumN(key: String) = ReadOnlyProperty<BotSettings, T?> { _, _ ->
        val value = getStringValueN(key)
        enumValues<T>().firstOrNull { value.equals(it.toString(), true) }
    }

    fun <T> getValue(key: String, default: T?, convertor: (String) -> T): T {
        val value = getStringValueN(key, default)
        return if (value != null) convertor(value)
        else default ?: throw IllegalStateException()
    }

    fun getStringValue(key: String) = getStringValueN(key, null) ?: throw IllegalStateException()
    fun getStringValueN(key: String, default: Any?): String? {
        val value = getStringValueN(key)
        if (value == null && default == null) throw IllegalStateException("missing env value for key: ${group}_${key}")
        return value
    }

    fun getStringValueN(key: String): String? {
        val finalKey = if (splitOnCammelCase) {
            key.camelToSnake()
        } else {
            key
        }
        return dotEnv[transformedGroup + "_" + finalKey.uppercase()]
    }
}