package me.melijn.kordkommons.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

object TimeUtil {

    /**
     * @return [LocalDateTime] instance with date and time when called
     */
    fun localDateTimeNow(): LocalDateTime = java.time.LocalDateTime.now().toKotlinLocalDateTime()

    /**
     * Positive duration if time2 occurred after time1
     * @return [Duration] between [time1] and [time2]
     */
    fun durationBetween(time1: Instant, time2: Instant): Duration {
        return java.time.Duration.between(time1.toJavaInstant(), time2.toJavaInstant()).toKotlinDuration()
    }

    /**
     * Use with @Serializable(with = [TimeUtil.DurationSerializer]) on fields or
     *
     * [Json.encodeToString] ([TimeUtil.DurationSerializer], 5.seconds) on duration objects
     *
     * [Json.decodeFromString] ([TimeUtil.DurationSerializer], durationJsonString) to get duration objects
     */
    object DurationSerializer : KSerializer<Duration> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Millis", PrimitiveKind.LONG)

        override fun deserialize(decoder: Decoder): Duration {
            return java.time.Duration.ofMillis(decoder.decodeLong()).toKotlinDuration()
        }

        override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Duration) {
            encoder.encodeLong(value.inWholeMilliseconds)
        }
    }
}