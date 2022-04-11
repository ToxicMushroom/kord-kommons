import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.Json
import me.melijn.kordkommons.utils.TimeUtil
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class TimeUtilTest {

    @Test
    fun test() {
        val t1 = Instant.now().toKotlinInstant()
        runBlocking { delay(5) }
        val t2 = Instant.now().toKotlinInstant()
        val duration = TimeUtil.durationBetween(t1, t2)
        val durationReverse = TimeUtil.durationBetween(t2, t1)
        assertEquals(true, duration.isPositive())
        assertEquals(false, durationReverse.isPositive())

        val fiveHours = 5.hours
        val encoded = Json.encodeToString(TimeUtil.DurationSerializer, fiveHours)
        val decoded = Json.decodeFromString(TimeUtil.DurationSerializer, encoded)
        assertEquals(fiveHours.inWholeMilliseconds, decoded.inWholeMilliseconds)
    }
}