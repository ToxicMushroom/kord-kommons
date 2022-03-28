package me.melijn.apkord.createtable

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import me.melijn.apkord.util.appendLine
import me.melijn.apkord.util.appendText

class TableProcessor(
    codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val location: String,

) : SymbolProcessor{

    val createTablesModuleFile =
        codeGenerator.createNewFile(Dependencies(false), location, "CreateTablesModule")

    init {
        createTablesModuleFile.appendLine("package ${location}\n")
        createTablesModuleFile.appendLine(
            """
                import org.jetbrains.exposed.sql.SchemaUtils
               """.trimIndent()
        )
        createTablesModuleFile.appendLine("object CreateTablesModule {\n")
        createTablesModuleFile.appendLine("    fun createTables() {")
        createTablesModuleFile.appendLine("        SchemaUtils.create(")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(CreateTable::class.java.name).toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(CreateTableVisitor(), Unit) }
        val ret = symbols.filter { !it.validate() }.toList()

        if (symbols.isNotEmpty()) {
            createTablesModuleFile.appendLine("        )")
            createTablesModuleFile.appendLine("    }")
            createTablesModuleFile.appendLine("}")
            createTablesModuleFile.close()
        }
        return ret
    }

    inner class CreateTableVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val className = classDeclaration.qualifiedName?.asString()?: throw IllegalStateException("Annotation not on class ?")
            createTablesModuleFile.appendLine("            $className,")
        }
    }
}