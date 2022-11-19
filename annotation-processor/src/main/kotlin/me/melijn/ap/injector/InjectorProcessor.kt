package me.melijn.ap.injector

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
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
    var injectLines = mutableListOf<String>()

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

            process.forEach { it.accept(InjectorVisitor(singleLines, injectLines), Unit) }
            injectKoinModuleFile.appendLine(singleLines.joinToString("\n"))

            injectKoinModuleFile.appendLine("    }")
            injectKoinModuleFile.appendLine("    override fun initInjects() {")
            injectKoinModuleFile.appendLine(injectLines.joinToString("\n"))
            injectKoinModuleFile.appendLine("    }")
            injectKoinModuleFile.appendLine("}")
            injectKoinModuleFile.close()
            count++
        }

        return ret
    }

    inner class InjectorVisitor(private val singleLines: MutableList<String>, private val injectLines: MutableList<String>) : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if (classDeclaration.classKind !in listOf(ClassKind.ANNOTATION_CLASS, ClassKind.INTERFACE))
                classDeclaration.primaryConstructor!!.accept(this, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            val parent = function.parentDeclaration as KSClassDeclaration
            val annotation = parent.annotations.firstOrNull { it.shortName.asString()==Inject::class.java.simpleName } ?: return

            val className = parent.qualifiedName?.asString() ?: throw IllegalStateException("Annotation not on class ?")
            singleLines.add("         single { $className(${function.parameters.joinToString(", ") { "get()" }}) } bind $className::class\n")

            val create = annotation.arguments.firstOrNull()?.value as Boolean?
            if (create == true) {
                val varName = "s${injectLines.size}"
                val line = initPlaceholder
                    .replace("%varName%", varName)
                    .replace("%className%", className)
                injectLines.add(line)
            }
        }
    }
}