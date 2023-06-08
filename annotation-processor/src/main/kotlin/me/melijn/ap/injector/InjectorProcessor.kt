package me.melijn.ap.injector

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import me.melijn.ap.util.appendLine

internal class InjectorProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val location: String,
    val extraImports: String,
    val extraInterfaces: String,
    val initPlaceholder: String
) : SymbolProcessor {

    var count = 0

    var singleLines = mutableListOf<String>()
    var initGroups = mutableMapOf<Int, Set<String>>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Inject::class.java.name).toList()
        val ret = symbols.filter { !it.validate() }.toList()

        val process = symbols
            .filter { it is KSClassDeclaration && it.validate() }

        if (process.isNotEmpty()) {
            val injectKoinModuleFile =
                codeGenerator.createNewFile(Dependencies(false), location, "InjectionKoinModule${count}")

            injectKoinModuleFile.appendLine("package ${location}\n")
            injectKoinModuleFile.appendLine(
                """
            import ${InjectorInterface::class.java.name}
            import org.koin.dsl.bind
            import org.koin.dsl.module
            $extraImports

            """.trimIndent()
            )

            val extraInterfacesFormatted = if (extraInterfaces.isBlank()) "" else ", $extraInterfaces"
            injectKoinModuleFile.appendLine("class InjectionKoinModule${count} : ${InjectorInterface::class.java.simpleName}() $extraInterfacesFormatted {\n")
            injectKoinModuleFile.appendLine("    override val module = module {")

            process.forEach {
                val injection = it.getAnnotationsByType(Inject::class).first()
                it.accept(InjectorVisitor(injection), Unit)
            }
            injectKoinModuleFile.appendLine(singleLines.joinToString("\n"))

            injectKoinModuleFile.appendLine("    }")
            injectKoinModuleFile.appendLine("    override fun initInjects(initGroup: Int) {")
            injectKoinModuleFile.appendLine(initGroups.entries.joinToString("\n") { (initGroup, initLines) ->
                "        if (initGroup == ${initGroup}) {\n" +
                        initLines.joinToString("\n") { " ".repeat(12) + it } +
                        "\n        }"
            })
            injectKoinModuleFile.appendLine("    }")
            injectKoinModuleFile.appendLine("}")
            injectKoinModuleFile.close()
            count++
        }

        return ret
    }

    inner class InjectorVisitor(
        private val injection: Inject
    ) : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val declaration = classDeclaration.primaryConstructor!!

            val className = classDeclaration.qualifiedName?.asString()
                ?: throw IllegalStateException("Annotation not on class ?")
            singleLines.add("         single { $className(${declaration.parameters.joinToString(", ") { "get()" }}) } bind $className::class\n")

            val create = injection.init
            val initGroup = injection.initGroup

            if (create) {
                val varName = "s${initGroups.size}"
                val line = initPlaceholder
                    .replace("%varName%", varName)
                    .replace("%className%", className)

                val initLines = initGroups[initGroup]?.toMutableSet() ?: mutableSetOf()
                initLines.add(line)
                initGroups[initGroup] = initLines
            }
        }
    }
}