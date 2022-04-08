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
        val camelCaseVar by stringN("camelCaseVar")
        val list by stringList("lijst")
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
        injectEnvironmentVariable("TEST_CAMEL_CASE_VAR", "hi")
        injectEnvironmentVariable("TEST_LIJST0", "0")
        injectEnvironmentVariable("TEST_LIJST1", "1")
        injectEnvironmentVariable("TEST_LIJST3", "3")

        BotSettings.globalSplitOnCammelCase = true
        val settings = SettingsToTest()
        assertEquals(token, settings.botToken)
        assertEquals(name, settings.name)
        assertEquals(id, settings.id)
        assertEquals(Classes.Knight, settings.clas)
        assertEquals(null, settings.nullableSetting)
        assertEquals("setanyway", settings.nullableSetting2)
        assertEquals("hi", settings.camelCaseVar)
        assertEquals(true, settings.list.isNotEmpty())
        assertEquals(2, settings.list.size)
        assertEquals("0", settings.list[0])
        assertEquals("1", settings.list[1])
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