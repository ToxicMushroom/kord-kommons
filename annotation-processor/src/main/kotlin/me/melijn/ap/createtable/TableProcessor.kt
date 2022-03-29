package me.melijn.ap.createtable

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import me.melijn.ap.util.appendLine

class TableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val location: String,
) : SymbolProcessor{

    private var count = 0
    var lines = mutableListOf<String>()

    init {
        logger.info("generating table file..")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(CreateTable::class.java.name).toList()
        val ret = symbols.filter { !it.validate() }.toList()

        val process = symbols
            .filter { it is KSClassDeclaration && it.validate() }

        if (process.isNotEmpty()) {
            val createTablesModuleFile =
                codeGenerator.createNewFile(Dependencies(false), location, "CreateTablesModule${count}")
            createTablesModuleFile.appendLine("package ${location}\n")
            createTablesModuleFile.appendLine(
                """
                import org.jetbrains.exposed.sql.SchemaUtils
               """.trimIndent()
            )
            createTablesModuleFile.appendLine("class CreateTablesModule${count} : ${CreateTableInterface::class.java.name} {\n")
            createTablesModuleFile.appendLine("    override fun createTables() {")
            createTablesModuleFile.appendLine("        SchemaUtils.create(")

            process.forEach { it.accept(CreateTableVisitor(lines), Unit) }
            createTablesModuleFile.appendLine(lines.joinToString("\n"))

            createTablesModuleFile.appendLine("        )")
            createTablesModuleFile.appendLine("    }")
            createTablesModuleFile.appendLine("}")
            createTablesModuleFile.close()

            logger.info("round $count processed ${lines.size} annotations")
        }

        return ret
    }

    inner class CreateTableVisitor(private val lines: MutableList<String>) : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val className = classDeclaration.qualifiedName?.asString()?: throw IllegalStateException("Annotation not on class ?")
            lines.add("            $className,")
        }
    }
}