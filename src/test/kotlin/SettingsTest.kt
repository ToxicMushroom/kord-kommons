import me.melijn.kordkommons.environment.BotSettings
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.test.assertEquals

class SettingsTest {

    inner class SettingsToTest : BotSettings("test") {
        val botToken by string("token")
        val name by string("name")
        val id by long("id")
        val clas by enum<Classes>("class", "Knight")
    }

    enum class Classes {
        Knight, Marauder, Assassin, Mage
    }

    @Test
    fun test() {
        val token = "lhasfhdsaofhwerfhewfihsa"
        val name = "merlin"
        val id = 231459866630291459
        val env = getModifiableEnvironment() // used to inject env variables into the java System.getEnv stuff
        env["TEST_TOKEN"] = token
        assertEquals(System.getenv("TEST_TOKEN"), token) // test the tests :)
        env["TEST_NAME"] = name
        env["TEST_ID"] = id.toString()
        env["TEST_CLASS"] = "Knight"

        val settings = SettingsToTest()
        assertEquals(settings.botToken, token)
        assertEquals(settings.name, name)
        assertEquals(settings.id, id)
        assertEquals(settings.clas, Classes.Knight)
    }

    /** janky hack mate: https://www.youtube.com/watch?v=OdfemrK97IM **/
    private fun getModifiableEnvironment(): MutableMap<String, String> {
        val pe = Class.forName("java.lang.ProcessEnvironment")
        val getenv: Method = pe.getDeclaredMethod("getenv")
        getenv.isAccessible = true
        val unmodifiableEnvironment: Any = getenv.invoke(null)
        val map = Class.forName("java.util.Collections\$UnmodifiableMap")
        val m: Field = map.getDeclaredField("m")
        m.isAccessible = true
        return m.get(unmodifiableEnvironment) as MutableMap<String, String>
    }

}