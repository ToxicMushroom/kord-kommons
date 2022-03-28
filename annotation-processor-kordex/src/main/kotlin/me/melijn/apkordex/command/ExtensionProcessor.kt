package me.melijn.apkordex.command

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import me.melijn.apkordex.util.appendLine

class ExtensionProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val location: String
) : SymbolProcessor {

    var count = 0

    var lines = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(KordExtension::class.java.name).toList()
        val ret = symbols.filter { !it.validate() }.toList()

        val process = symbols
            .filter { it is KSClassDeclaration && it.validate() }

        if (process.isNotEmpty()) {

            val injectKoinModuleFile =
                codeGenerator.createNewFile(Dependencies(false), location, "ExtensionAdderModule${count}")

            injectKoinModuleFile.appendLine("package $location\n")

            injectKoinModuleFile.appendLine("import " + ExtensionInterface::class.java.name)

            injectKoinModuleFile.appendLine("\nclass ExtensionAdderModule${count} : ${ExtensionInterface::class.java.simpleName}() {\n")
            injectKoinModuleFile.appendLine("    override val list = listOf(")

            process.forEach { it.accept(InjectorVisitor(lines), Unit) }
            injectKoinModuleFile.appendLine(lines.joinToString(",\n"))

            injectKoinModuleFile.appendLine("    )")
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
            lines.add("         $className()")
        }
    }
}