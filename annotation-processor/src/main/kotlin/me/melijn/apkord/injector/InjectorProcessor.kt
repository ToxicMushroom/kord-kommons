package me.melijn.apkord.injector

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import me.melijn.apkord.util.appendLine

class InjectorProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val location: String
) : SymbolProcessor {

    var count = 0

    var lines = mutableListOf<String>()

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
           
           """.trimIndent()
            )
            injectKoinModuleFile.appendLine("class InjectionKoinModule${count} : ${InjectorInterface::class.java.simpleName}() {\n")
            injectKoinModuleFile.appendLine("    override val module = module {")

            process.forEach { it.accept(InjectorVisitor(lines), Unit) }
            injectKoinModuleFile.appendLine(lines.joinToString("\n"))

            injectKoinModuleFile.appendLine("    }")
            injectKoinModuleFile.appendLine("}")
            injectKoinModuleFile.close()
            count++
        }


        return ret
    }


    inner class InjectorVisitor(private val lines: MutableList<String>) : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            classDeclaration.primaryConstructor!!.accept(this, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            val parent = function.parentDeclaration as KSClassDeclaration

            val className = parent.qualifiedName?.asString() ?: throw IllegalStateException("Annotation not on class ?")
            lines.add("         single { $className(${function.parameters.joinToString(", ") { "get()" }}) } bind $className::class\n")
        }
    }
}