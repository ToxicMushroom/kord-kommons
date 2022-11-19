package me.melijn.ap.settings

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import me.melijn.ap.util.Reflections
import me.melijn.ap.util.appendText
import me.melijn.kordkommons.environment.BotSettings
import java.util.*

internal class SettingsProcessor(
    codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val location: String
) : SymbolProcessor {

    private val settingsFile =
        codeGenerator.createNewFile(Dependencies(false), location, "Settings")
    private val settingsImports = StringBuilder()
    val settings = StringBuilder()

    init {
        settingsImports.appendLine("package ${location}\n")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(SettingsTemplate::class.java.name).toList()


        settings.appendLine("object Settings {")
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { ksAnnotated ->
                settingsImports.appendLine("import ${BotSettings::class.java.name}")

                ksAnnotated.annotations.firstOrNull {
                    it.shortName.asString() == SettingsTemplate::class.java.simpleName
                }?.arguments?.firstOrNull()?.value?.let { arg0 ->
                    settingsImports.appendLine(arg0.toString())
                }

                ksAnnotated.accept(SettingVisitor(), Unit)
            }


        val ret = symbols.filter { !it.validate() }.toList()

        if (symbols.isNotEmpty()) {
            settings.appendLine("}")
            settingsFile.appendText(settingsImports.toString() + "\n" + settings.toString())
        }
        return ret
    }

    inner class SettingVisitor : KSVisitorVoid() {

        @Suppress("UNCHECKED_CAST") // how does one check a cast then
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {


            val classes =
                classDeclaration.declarations.filter { it is KSClassDeclaration } as Sequence<KSClassDeclaration>
            var body = ""
            for (child in classes) {
                body += visitChildClassDeclaration(child, data)
            }

            val fields = getFieldsString(classDeclaration)

            settings.appendLine(body)
            settings.appendLine(fields)
        }

        @Suppress("UNCHECKED_CAST") // how does one check a cast then
        fun visitChildClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): String {
            val classes =
                classDeclaration.declarations.filter { it is KSClassDeclaration } as Sequence<KSClassDeclaration>
            var body = ""
            for (child in classes) {
                val innerBody = visitChildClassDeclaration(child, data)
                body += innerBody
            }
            return if (classes.count() == 0) {
                val code = Reflections.getCode(classDeclaration)
                code.replace("Declarations for info for ", "\n") +"\n"
            } else {
                val variables = getFieldsString(classDeclaration)
                val fullInnerCode = Reflections.getCode(classDeclaration)
                val firstPathName = fullInnerCode.lines().first { it.contains("BotSettings") }
                    .replace(".*BotSettings\\(\"(\\w+)\"\\).*".toRegex()) { res ->
                        res.groups[1]!!.value
                    }
                val className = classDeclaration.simpleName.asString()
                "    class $className : BotSettings(\"$firstPathName\") {\n" +
                    body.replace("BotSettings\\(\"(\\w+)\"\\)".toRegex()) { res ->
                        "BotSettings(\"${firstPathName}_${res.groups[1]!!.value}\")"
                    } + variables + "\n}\n"

            }
        }

        private fun getFieldsString(classDeclaration: KSClassDeclaration): String {
            return classDeclaration.declarations.joinToString("\n") { clazz ->
                val className = clazz.simpleName.asString()
                val variableName = className.replaceFirstChar { it.lowercase(Locale.getDefault()) }
                if (variableName == "<init>") ""
                else "val $variableName = $className()"
            }
        }
    }

}