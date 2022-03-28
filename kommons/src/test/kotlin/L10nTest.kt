import me.melijn.kordkommons.translation.L10n
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class L10nTest {

    val te = "te"
    @Test
    fun test() {
       val translated = L10n.translate(Locale.US, "${te}st")
       val translatedDutch = L10n.translate(Locale("nl", "NL"), "${te}st")
       val translatedDutchBelgian = L10n.translate(Locale("nl", "BE"), "${te}st")
       assertEquals(translated, "This is a test message")
       assertEquals(translatedDutch, "Dit is een test bericht")
       assertEquals(translatedDutchBelgian, "iets vlaams")
    }
}