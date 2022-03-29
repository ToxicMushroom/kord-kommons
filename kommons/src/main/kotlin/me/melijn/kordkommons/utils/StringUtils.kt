package me.melijn.kordkommons.utils

import java.lang.StringBuilder
import kotlin.math.pow


val SPACE_PATTERN = Regex("\\s+")

object StringUtils {

    fun humanReadableByteCountBin(bytes: Int): String = humanReadableByteCountBin(bytes.toLong())
    fun humanReadableByteCountBin(bytes: Long): String {
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 0xfffccccccccccccL shr 40 -> String.format("%.3f KiB", bytes / 2.0.pow(10.0))
            bytes < 0xfffccccccccccccL shr 30 -> String.format("%.3f MiB", bytes / 2.0.pow(20.0))
            bytes < 0xfffccccccccccccL shr 20 -> String.format("%.3f GiB", bytes / 2.0.pow(30.0))
            bytes < 0xfffccccccccccccL shr 10 -> String.format("%.3f TiB", bytes / 2.0.pow(40.0))
            bytes < 0xfffccccccccccccL -> String.format("%.3f PiB", (bytes shr 10) / 2.0.pow(40.0))
            else -> String.format("%.3f EiB", (bytes shr 20) / 2.0.pow(40.0))
        }
    }
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