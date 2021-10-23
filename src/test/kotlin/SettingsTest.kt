import me.melijn.kordkommons.environment.BotSettings
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import kotlin.test.assertEquals

class SettingsTest {

    inner class SettingsToTest : BotSettings("test") {
        val botToken by string("token")
        val name by string("name")
        val id by long("id")
        val clas by enum<Classes>("class", "Knight")
        val nullableSetting by stringN("nullable")
        val nullableSetting2 by stringN("nullable2")
    }

    enum class Classes {
        Knight, Marauder, Assassin, Mage
    }

    @Test
    fun test() {
        val token = "lhasfhdsaofhwerfhewfihsa"
        val name = "merlin"
        val id = 231459866630291459

        injectEnvironmentVariable("TEST_TOKEN", token)
        assertEquals(System.getenv("TEST_TOKEN"), token) // test the tests :)
        injectEnvironmentVariable("TEST_NAME", name)
        injectEnvironmentVariable("TEST_ID", id.toString())
        injectEnvironmentVariable("TEST_CLASS", "Knight")
        injectEnvironmentVariable("TEST_NULLABLE2", "setanyway")

        val settings = SettingsToTest()
        assertEquals(settings.botToken, token)
        assertEquals(settings.name, name)
        assertEquals(settings.id, id)
        assertEquals(settings.clas, Classes.Knight)
        assertEquals(settings.nullableSetting, null)
        assertEquals(settings.nullableSetting2, "setanyway")
    }

    /** janky hack mate: https://www.youtube.com/watch?v=OdfemrK97IM **/
    @Throws(Exception::class)
    fun injectEnvironmentVariable(key: String, value: String) {
        val processEnvironment = Class.forName("java.lang.ProcessEnvironment")
        val treeMap = getAccessibleField(processEnvironment, "theCaseInsensitiveEnvironment")
        val unmodifiableMap = treeMap[null]
        injectIntoTreeMap(key, value, unmodifiableMap)
    }

    @Throws(NoSuchFieldException::class)
    private fun getAccessibleField(clazz: Class<*>, fieldName: String): Field {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field
    }

    @Throws(ReflectiveOperationException::class)
    private fun injectIntoTreeMap(key: String, value: String, map: Any) {
        val unmodifiableMap = Class.forName("java.util.TreeMap")
        unmodifiableMap
            .getDeclaredMethod("put", Object::class.java, Object::class.java)
            .invoke(map, key, value)
    }


}