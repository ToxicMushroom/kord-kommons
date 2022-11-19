package me.melijn.kordkommons.logger

import mu.KLogger
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

public inline fun <reified R : Any> R.logger(): KLogger =
    KotlinLogging.logger(LoggerFactory.getLogger(this::class.java.name.substringBefore("\$Companion")))

public object Log {
    public operator fun getValue(thisRef: Any?, prop: KProperty<*>): KLogger {
        return if (thisRef != null) {
            val underlyingLogger = LoggerFactory.getLogger(thisRef::class.java)
            KotlinLogging.logger(underlyingLogger)
        } else {
            // reflection :)
            val invoke = prop::class.java.getMethod("getOwner").invoke(prop)
            val clazz = invoke::class.java.getMethod("getJClass").invoke(invoke) as Class<*>

            KotlinLogging.logger(LoggerFactory.getLogger(clazz.name))
        }
    }
}