package me.melijn.kordkommons.utils

import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow


val SPACE_PATTERN = Regex("\\s+")
const val SPLIT_HINT = "`SPLIT_HERE`"

object StringUtils {

    fun humanReadableByteCountBin(bytes: Int, locale: Locale = Locale.getDefault()): String =
        humanReadableByteCountBin(bytes.toLong(), locale)

    /**
     * Formats byte count into the closest most readable format (e.g. KibiByte, MibiByte, ...)
     * Everything after 3 decimal places is cut off
     * @param bytes byte count to format
     * @param locale optional locale for the decimal separator that's used
     *
     * example: humanReadableByteCountBin(4121672, Locale("us")) => "3.931 MiB"
     *
     * @return formatted string
     */
    fun humanReadableByteCountBin(bytes: Long, locale: Locale = Locale.getDefault()): String {
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 0xfffccccccccccccL shr 40 -> String.format(locale, "%.3f KiB", bytes / 2.0.pow(10.0))
            bytes < 0xfffccccccccccccL shr 30 -> String.format(locale, "%.3f MiB", bytes / 2.0.pow(20.0))
            bytes < 0xfffccccccccccccL shr 20 -> String.format(locale, "%.3f GiB", bytes / 2.0.pow(30.0))
            bytes < 0xfffccccccccccccL shr 10 -> String.format(locale, "%.3f TiB", bytes / 2.0.pow(40.0))
            bytes < 0xfffccccccccccccL -> String.format(locale, "%.3f PiB", (bytes shr 10) / 2.0.pow(40.0))
            else -> String.format(locale, "%.3f EiB", (bytes shr 20) / 2.0.pow(40.0))
        }
    }

    /**
     * Tries to find an index in [text] between [splitAtLeast] and [text].length to split on,
     *   splits are preferred on punctuation like . , ! ? and spaces but above all [SPLIT_HINT].
     * @param text text in which an index needs to be found.
     * @param splitAtLeast minimum split index, will be ignored if no good split places are found.
     *
     * @return best split index
     */
    fun getSplitIndex(text: String, splitAtLeast: Int): Int {
        var index = text.lastIndexOf(SPLIT_HINT)
        if (index < splitAtLeast) index = text.lastIndexOf("\n")
        if (index < splitAtLeast) index = text.lastIndexOf(". ")
        if (index < splitAtLeast) index = text.lastIndexOf(" ")
        if (index < splitAtLeast) index = text.lastIndexOf(",")
        if (index < splitAtLeast) index = text.lastIndexOf("? ")
        if (index < splitAtLeast) index = text.lastIndexOf("! ")
        if (index < splitAtLeast) index = text.lastIndexOf("-")
        if (index < splitAtLeast) index = text.length - 1
        return index
    }

    /**
     * Splits string into pieces, will make sure each piece is between [splitAtLeast] and [maxLength]
     * furthermore concatenating all strings in the returned list will equal the [message].
     * Splits prefer punctuation and spaces.
     *
     * @param message message to chop into pieces
     * @param splitAtLeast minimum piece size
     * @param maxLength maximum piece size
     *
     * @return list of pieces
     */
    fun splitMessage(message: String, splitAtLeast: Int = 1800, maxLength: Int = 2000): List<String> {
        var msg = message
        val messages = ArrayList<String>()
        while (msg.length > maxLength) {
            val findLastNewline = msg.substring(0, maxLength - 1)

            val index = getSplitIndex(findLastNewline, splitAtLeast)

            messages.add(msg.substring(0, index))
            msg = msg.substring(index)
        }
        if (msg.isNotEmpty()) messages.add(msg)
        return messages
    }

    fun splitMessageWithCodeBlocks(
        message: String,
        splitAtLeast: Int = 1800,
        maxLength: Int = 1970,
        language: String? = null
    ): List<String> {
        val lang = language ?: message.drop(3).takeWhile { it != '\n' }.takeIf { !it.contains(" ") } ?: ""
        val msg = message.removePrefix("```${lang}").removeSuffix("```")
        return splitMessage(msg, splitAtLeast, maxLength).map { "```${lang}\n${it}```" }
    }
}

/**
 * @return whether the string contains an [Int]
 */
fun String.isNumber(): Boolean {
    return toIntOrNull() != null
}

/**
 * @return whether the string contains an [Int] and that the value of that int is >= 0
 */
fun String.isPositiveNumber(): Boolean {
    val number = toIntOrNull()
    return number != null && number >= 0
}

/**
 * @return whether the string contains an [Int] and that the value of that int is <= 0
 */
fun String.isNegativeNumber(): Boolean {
    val number = toIntOrNull()
    return number != null && number <= 0
}

/**
 * @return whether the string contains an [Int] and that the value of that int is > 0
 */
fun String.isStrictPositiveNumber(): Boolean {
    val number = toIntOrNull()
    return number != null && number > 0
}

/**
 * @return whether the string contains an [Int] and that the value of that int is < 0
 */
fun String.isStrictNegativeNumber(): Boolean {
    val number = toIntOrNull()
    return number != null && number < 0
}

fun String.remove(vararg strings: String, ignoreCase: Boolean = false): String {
    var newString = this
    for (string in strings) {
        newString = newString.replace(string, "", ignoreCase)
    }
    return newString
}

fun String.removeFirst(vararg strings: String, ignoreCase: Boolean = false): String {
    var newString = this
    for (string in strings) {
        newString = newString.replaceFirst(string, "", ignoreCase)
    }
    return newString
}

fun String.removeFirst(vararg regexes: Regex): String {
    var newString = this
    for (regex in regexes) {
        newString = newString.replaceFirst(regex, "")
    }
    return newString
}

fun String.removePrefix(prefix: CharSequence, ignoreCase: Boolean = false): String {
    if (startsWith(prefix, ignoreCase)) {
        return substring(prefix.length)
    }
    return this
}

fun String.splitIETEL(delimiter: String): List<String> {
    val res = this.split(delimiter)
    return if (res.first().isEmpty() && res.size == 1) {
        emptyList()
    } else {
        res
    }
}

fun String.escapeMarkdown(): String {
    return this.replace("*", "\\*")
        .replace("||", "\\|\\|")
        .replace("_", "\\_")
        .replace("~~", "\\~\\~")
        .replace("> ", "\\> ")
        .replace("`", "'")
}

fun String.escapeCodeBlock(): String {
    return this.replace("`", "'")
}

fun String.toUpperWordCase(): String {
    var previous = ' '
    var newString = ""
    this.toCharArray().forEach { c: Char ->
        newString += if (previous == ' ') c.uppercase() else c.lowercase()
        previous = c
    }
    return newString
}

fun Int.toHexString(size: Int = 6): String {
    return String.format("#%0${size}X", 0xFFFFFF and this)
}

fun String.isInside(vararg stringList: String, ignoreCase: Boolean): Boolean {
    return stringList.any { it.equals(this, ignoreCase) }
}

fun String.isInside(stringList: Collection<String>, ignoreCase: Boolean): Boolean {
    return stringList.any { it.equals(this, ignoreCase) }
}

fun String.camelToSnake(): String {
    val camelIndexes = mutableListOf<Int>()
    forEachIndexed { index, c ->
        if (index != 0 && c.isUpperCase() && !this[index - 1].isUpperCase()) camelIndexes.add(index)
    }
    val snakeBuilder = StringBuilder(this)
    camelIndexes.forEachIndexed { index, i -> snakeBuilder.insert(index + i, "_") }
    return snakeBuilder.toString()
}

fun ansiFormat(colorCode: String) = "\u001B[0;${colorCode}m"