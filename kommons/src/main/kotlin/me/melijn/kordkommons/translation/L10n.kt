package me.melijn.kordkommons.translation

import org.jetbrains.annotations.PropertyKey
import java.text.FieldPosition
import java.text.MessageFormat
import java.util.*

const val BUNDLE = "messages.bundle"

object L10n {
    fun translate(locale: Locale, @PropertyKey(resourceBundle = BUNDLE) key: String): String =
        ResourceBundle.getBundle(BUNDLE, locale).getString(key)

    fun translate(locale: Locale, @PropertyKey(resourceBundle = BUNDLE) key: String, params: Array<out Any?>): String =
        MessageFormat(translate(locale, key), locale).format(params, StringBuffer(64), FieldPosition(0)).toString()
}