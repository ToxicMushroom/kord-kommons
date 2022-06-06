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
        return findAllClassesUsingClassLoader(packageName)
            .filterNotNull()
            .filter { it.toString().contains(baseClassName, true) && !it.toString().contains("$") }
            .maxByOrNull {
                it.name.replace(".*${baseClassName}(\\d+)".toRegex()) { res ->
                    res.groups[1]?.value ?: ""
                }.toInt()
            } ?: throw IllegalArgumentException("Couldn't resolve a class object for $baseClassName in $packageName")
    }

    fun findAllClassesUsingClassLoader(packageName: String): Sequence<Class<*>?> {
        val stream = ClassLoader.getSystemClassLoader()
            .getResourceAsStream(packageName.replace("[.]".toRegex(), "/"))
            ?: return emptySequence()

        val reader = BufferedReader(InputStreamReader(stream))

        val stream2 = ClassLoader.getSystemClassLoader()
            .getResourceAsStream("/" + packageName.replace("[.]".toRegex(), "/"))
            ?: return emptySequence()
        logger.debug { "Found files in /$packageName" }

        return reader.lineSequence()
            .also { logger.debug { "Found files in $packageName: ${it.joinToString()}" } }
            .filter { line: String -> line.endsWith(".class") }
            .also { logger.debug { "Found classes in $packageName: ${it.joinToString()}" } }
            .map { line: String ->
                getClass(
                    line,
                    packageName
                )
            }
            .filter { it != null }
            .also { logger.debug { "Turned into Class Objects from $packageName: ${it.joinToString()}" } }
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