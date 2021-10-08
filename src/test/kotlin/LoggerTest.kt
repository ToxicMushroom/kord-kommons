import me.melijn.kordkommons.logger.logger
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class LoggerTest {

    @Test
    fun test() {
        val ogOut = System.out
        val baos = ByteArrayOutputStream()
        val capture = PrintStream(baos)
        System.setOut(capture)

        val logger = logger()
        logger.info("hello") // output will go into the baos stream

        /** Tests **/
        assert(baos.toString().contains("INFO"))
        assert(baos.toString().contains(LoggerTest::class.simpleName!!))
        assert(baos.toString().contains("hello"))

        System.setOut(ogOut)
        println(baos.toString())
    }
}