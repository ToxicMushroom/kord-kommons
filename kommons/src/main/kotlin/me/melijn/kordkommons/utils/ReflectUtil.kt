package me.melijn.kordkommons.utils

import me.melijn.kordkommons.logger.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object ReflectUtil {

    val logger by Log

    /**
     * @param packageName package the generated classes resides in
     * @param baseClassName only the className without the appended number
     * @param params params of the desired constructor
     * @param values values to fill into the desired constructor
     *
     * @throws IllegalArgumentException if the arguments don't resolve a class
     * @throws ClassCastException if the inferred type doesn't match the classType
     *
     * @return the object of the resolved class
     */
    fun <T> getInstanceOfKspClass(
        packageName: String,
        baseClassName: String,
        params: Array<Class<*>> = arrayOf(),
        values: Array<Any> = arrayOf()
    ): T {
        return findCompleteGeneratedKspClass(packageName, baseClassName)
            .getConstructor(*params)
            .newInstance(*values) as T
    }

    /**
     * @param packageName package the generated classes resides in
     * @param baseClassName only the className without the appended number
     *
     * @throws IllegalArgumentException if the arguments don't resolve a class
     *
     * @return the class object of the resolved class
     */
    fun findCompleteGeneratedKspClass(packageName: String, baseClassName: String): Class<*> {
        var i = 0
        val sysCl = ClassLoader.getSystemClassLoader()
        while (sysCl.loadClass("$packageName.$baseClassName${i + 1}") != null) { i++ }
        return sysCl.loadClass("$packageName.$baseClassName${i}")
    }

    /**
     * Broken for jar files
     */
    fun findAllClassesUsingClassLoader(packageName: String): Sequence<Class<*>?> {
        val stream = ClassLoader.getSystemClassLoader()
            .getResourceAsStream(packageName.replace("[.]".toRegex(), "/"))
            ?: return emptySequence()

        val reader = BufferedReader(InputStreamReader(stream))

        return reader.lineSequence()
            .map { logger.debug { "Found file in $packageName: $it" }; it }
            .filter { line: String -> line.endsWith(".class") }
            .map { logger.debug { "Found class in $packageName: $it" }; it }
            .map { line: String ->
                getClass(
                    line,
                    packageName
                )
            }
            .filter { it != null }
            .map { logger.debug { "Turned into Class Object from $packageName: $it" }; it }
    }

    private fun getClass(className: String, packageName: String): Class<*>? {
        try {
            return Class.forName(
                packageName + "."
                        + className.substring(0, className.lastIndexOf('.'))
            )
        } catch (e: ClassNotFoundException) {
            // handle the exception
        }
        return null
    }
}