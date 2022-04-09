import me.melijn.kordkommons.utils.StringUtils
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StringUtilTest {

    @Test
    fun test() {
        assertEquals("10.348 GiB", StringUtils.humanReadableByteCountBin(11111111111))
        assertEquals("31.044 GiB", StringUtils.humanReadableByteCountBin(33333333333))
        assertEquals("1.086 KiB", StringUtils.humanReadableByteCountBin(1112))

        val text =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Nibh nisl condimentum id venenatis a condimentum vitae sapien. Imperdiet proin fermentum leo vel orci porta non. Velit egestas dui id ornare arcu odio ut. Varius duis at consectetur lorem. Tortor dignissim convallis aenean et. Congue eu consequat ac felis donec et odio. Magnis dis parturient montes nascetur. Lobortis scelerisque fermentum dui faucibus. Sem integer vitae justo eget magna fermentum."
        val pieces = StringUtils.splitMessage(text, 100, 200)
        assertEquals(text, pieces.joinToString(""))
    }
}