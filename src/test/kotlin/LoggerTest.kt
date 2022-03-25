import me.melijn.kordkommons.logger.Log
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
        val logger2 by Log
        logger.info {"hello" } // output will go into the baos stream
        logger2.warn { "world" } // output will go into the baos stream

        /** Tests **/
        assert(baos.toString().contains("INFO"))
        assert(baos.toString().contains(LoggerTest::class.simpleName!!))
        assert(baos.toString().contains("hello"))

        assert(baos.toString().contains("WARN"))
        assert(baos.toString().contains("world"))

        System.setOut(ogOut)
        println(baos.toString())
    }
}