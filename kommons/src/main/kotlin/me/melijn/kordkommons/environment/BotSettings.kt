package me.melijn.kordkommons.environment

import io.github.cdimascio.dotenv.Dotenv
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
public open class BotSettings(public val group: String) {
    private val transformedGroup = group.replace(".", "_").uppercase()
    public var splitOnCamelCase: Boolean = globalSplitOnCamelCase

    public companion object {
        public var dotEnv: Dotenv = dotenv {
            this.filename = System.getenv("ENV_FILE") ?: ".env"
            this.ignoreIfMissing = true
        }
        public var globalSplitOnCamelCase: Boolean = false
    }

    public fun stringList(
        key: String,
        default: List<String> = emptyList()
    ): ReadOnlyProperty<BotSettings, List<String>> =
        ReadOnlyProperty { _, _ ->
            var i = 0
            val list = mutableListOf<String>()
            var value = getStringValueN(key + i++)?.also { list.add(it) }
            while (value != null) {
                value = getStringValueN(key + i++)?.also { list.add(it) }
            }
            list.takeIf { it.isNotEmpty() } ?: default
        }

    public fun string(key: String, default: String? = null): ReadOnlyProperty<BotSettings, String> =
        primitive(key, default) { it }

    public fun stringN(key: String, default: String? = null): ReadOnlyProperty<BotSettings, String?> =
        primitiveN(key, default) { it }

    public fun long(key: String, default: Long? = null): ReadOnlyProperty<BotSettings, Long> =
        primitive(key, default) { it.toLong() }

    public fun longN(key: String, default: Long? = null): ReadOnlyProperty<BotSettings, Long?> =
        primitiveN(key, default) { it.toLong() }

    public fun int(key: String, default: Int? = null): ReadOnlyProperty<BotSettings, Int> =
        primitive(key, default) { it.toInt() }

    public fun intN(key: String, default: Int? = null): ReadOnlyProperty<BotSettings, Int?> =
        primitiveN(key, default) { it.toInt() }

    public fun boolean(key: String, default: Boolean? = null): ReadOnlyProperty<BotSettings, Boolean> =
        primitive(key, default) { it.toBoolean() }

    public fun booleanN(key: String, default: Boolean? = null): ReadOnlyProperty<BotSettings, Boolean?> =
        primitiveN(key, default) { it.toBoolean() }

    public fun float(key: String, default: Float? = null): ReadOnlyProperty<BotSettings, Float> =
        primitive(key, default) { it.toFloat() }

    public fun floatN(key: String, default: Float? = null): ReadOnlyProperty<BotSettings, Float?> =
        primitiveN(key, default) { it.toFloat() }

    public fun double(key: String, default: Double? = null): ReadOnlyProperty<BotSettings, Double> =
        primitive(key, default) { it.toDouble() }

    public fun doubleN(key: String, default: Double? = null): ReadOnlyProperty<BotSettings, Double?> =
        primitiveN(key, default) { it.toDouble() }

    private fun <T> primitive(key: String, default: T?, converter: (String) -> T) =
        ReadOnlyProperty<BotSettings, T> { _, _ -> getValue(key, default, converter) }

    private fun <T> primitiveN(key: String, default: T?, converter: (String) -> T) =
        ReadOnlyProperty<BotSettings, T?> { _, _ -> getValueN(key, default, converter) }

    public inline fun <reified T : Enum<T>> enum(
        key: String,
        default: String? = null
    ): ReadOnlyProperty<BotSettings, T> =
        ReadOnlyProperty { _, _ ->
            val value = getValue(key, default) { t -> t }
            val enumValues = enumValues<T>()
            enumValues.firstOrNull { value.equals(it.toString(), true) }
                ?: throw IllegalArgumentException(
                    "the env entry: ${group}_${key}=${value} has an incorrect value," +
                            " the value should be one of the following: " +
                            enumValues.joinToString()
                )
        }

    public inline fun <reified T : Enum<T>> enumN(key: String): ReadOnlyProperty<BotSettings, T?> =
        ReadOnlyProperty { _, _ ->
            val value = getStringValueN(key)
            enumValues<T>().firstOrNull { value.equals(it.toString(), true) }
        }

    /** Throws if (the default value is null and the value is missing) **/
    public fun <T> getValue(key: String, default: T?, convertor: (String) -> T): T =
        getValueN(key, default, convertor) ?: throw IllegalStateException("missing env value for key: ${group}_${key}")

    public fun <T> getValueN(key: String, default: T?, convertor: (String) -> T): T? {
        val value = getStringValueN(key)
        return if (value != null) convertor(value)
        else default
    }

    public fun getStringValueN(key: String): String? {
        val keyInChosenFormat = if (splitOnCamelCase) key.camelToSnake() else key

        return dotEnv[transformedGroup + "_" + keyInChosenFormat.uppercase()]
    }
}