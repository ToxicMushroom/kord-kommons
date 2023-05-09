package me.melijn.apredgres.cacheableimport com.google.devtools.ksp.getDeclaredPropertiesimport com.google.devtools.ksp.innerArgumentsimport com.google.devtools.ksp.processing.*import com.google.devtools.ksp.symbol.*import com.google.devtools.ksp.validateimport me.melijn.apredgres.util.Reflectionsimport me.melijn.apredgres.util.appendLineimport me.melijn.apredgres.util.appendTextimport me.melijn.kordkommons.utils.TimeUtilimport me.melijn.kordkommons.utils.UUIDUtilimport me.melijn.kordkommons.utils.removeimport me.melijn.kordkommons.utils.removeFirstimport org.intellij.lang.annotations.Languageimport java.io.OutputStreamimport java.util.*import kotlin.time.Durationinternal class CacheableProcessor(    val codeGenerator: CodeGenerator,    val logger: KSPLogger,    val location: String,    val globalKeyPrefix: String) : SymbolProcessor {    private val cacheableFile = codeGenerator.createNewFile(        Dependencies(false),        location, "CacheExtensions"    )    val imports = StringBuilder()    val sb = StringBuilder()    init {        imports.appendLine("package $location\n")        imports.appendLine(            """                import kotlinx.serialization.*                import org.jetbrains.exposed.sql.ResultRow            """.trimIndent()        )    }    override fun process(resolver: Resolver): List<KSAnnotated> {        val symbols = resolver.getSymbolsWithAnnotation(Cacheable::class.java.name).toList()        val ret = symbols.filter { !it.validate() }.toList()        symbols            .filter { symbol -> symbol is KSClassDeclaration && symbol.validate() }            .forEach { symbol ->                symbol.accept(InjectorVisitor(), Unit)            }        if (symbols.isNotEmpty()) {            cacheableFile.appendText(imports.toString())            cacheableFile.appendText(sb.toString())            cacheableFile.close()        }        return ret    }    inner class InjectorVisitor : KSVisitorVoid() {        var indexCount = -1        lateinit var classKeyPrefix: String        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {            val name = classDeclaration.packageName.asString() + "." + classDeclaration.simpleName.asString()            val simpleName = classDeclaration.simpleName.asString()            classKeyPrefix = "$globalKeyPrefix${simpleName.lowercase()}"            val properties = classDeclaration.getDeclaredProperties()                .filter { it.simpleName.asString() != "primaryKey" }            val pkeyProperty: KSPropertyDeclaration = classDeclaration.getDeclaredProperties().first {                it.type.resolve().toString() == "PrimaryKey"            }            val indexes = getIndexes(classDeclaration)            /** janky hack part, don't touch unless it broke pls **/            val field = pkeyProperty.javaClass.getDeclaredField("propertyDescriptor\$delegate")            field.isAccessible = true            val lazyPropertyDesciptor = field.get(pkeyProperty)            val lazyValueMethod = Lazy::class.java.getMethod("getValue")            val lazyValue = lazyValueMethod.invoke(lazyPropertyDesciptor)            val propertyDescriptor = lazyValue            val aaaa = lazyValue::class.java.getMethod("getSource").invoke(propertyDescriptor)            val fish = aaaa::class.java.getMethod("getPsi").invoke(aaaa)            val lastChildField = fish::class.java.getMethod("getLastChild")            val firstChildField = fish::class.java.getMethod("getFirstChild")            val last1 = lastChildField.invoke(fish)            val last2 = lastChildField.invoke(last1)            var invokeEl = firstChildField.invoke(last2)            val fieldList = mutableListOf<String>()            while (invokeEl != null) {                val nextSiblingField = invokeEl::class.java.getMethod("getNextSibling")                val type = invokeEl.toString()                if (type == "VALUE_ARGUMENT") {                    val first2 = firstChildField.invoke(invokeEl)                    val first3 = firstChildField.invoke(first2)                    val text = first3::class.java.getMethod("getText").invoke(first3)                    fieldList.add(text.toString())                }                invokeEl = try {                    nextSiblingField.invoke(invokeEl)                } catch (t: Throwable) {                    null                }            }            val pkeyProperties = classDeclaration.getDeclaredProperties()                .filter { it.simpleName.asString() != "primaryKey" }                .filter { fieldList.contains(it.simpleName.asString()) }            sb.appendLine("// $fieldList")            /** Cache object class **/            sb.appendLine("@Serializable")            sb.appendLine("data class ${simpleName}Data(")            sb.appendLine(properties.joinToString(",\n") {                val type = getType(it)                val annotation = when (type) {                    Duration::class.java.name -> "@Serializable(with = ${TimeUtil.DurationSerializer::class.java.canonicalName}::class)\n"                    UUID::class.java.name -> "@Serializable(with = ${UUIDUtil.UUIDSerializer::class.java.canonicalName}::class)\n"                    else -> ""                }                "$annotation    var " + it.simpleName.asString() + ": " + type            })            sb.appendLine(") {")            sb.appendLine("    companion object {")            sb.appendLine("        fun fromResRow(resRow: ResultRow): ${simpleName}Data {")            sb.appendLine("            return ${simpleName}Data(")            sb.appendLine(properties.joinToString(",\n") {                "                resRow[$name." + it.simpleName.asString() + "]${getValueGetter(it)}"            })            sb.appendLine("            )")            sb.appendLine("        }")            sb.appendLine("    }")            sb.appendLine("}\n")            val abstractPkg = "$location.database.manager"            val abstractMgrName = "Abstract${simpleName}Manager"            val dependencies = Dependencies(false)            val abstractManager = codeGenerator.createNewFile(dependencies, abstractPkg, abstractMgrName)            /** Do abstract mgr imports **/            abstractManager.appendLine(                """                package $abstractPkg                                import me.melijn.kordkommons.database.DBTableManager                import me.melijn.kordkommons.database.insertOrUpdate                import me.melijn.kordkommons.database.DriverManager                import $location.${simpleName}Data                import org.jetbrains.exposed.sql.select                import org.jetbrains.exposed.sql.and                import org.jetbrains.exposed.sql.deleteWhere                import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq                import $name                import kotlinx.serialization.*                import kotlinx.serialization.json.*                            """.trimIndent()            )            abstractManager.appendLine("open class Abstract${simpleName}Manager(override val driverManager: DriverManager) : DBTableManager<${simpleName}>(driverManager, ${simpleName}) {")            abstractManager.appendLine("    val classKeyPrefix = \"$classKeyPrefix\"")            addGetByIdMethod(abstractManager, pkeyProperties, simpleName, name, properties)            addGetCachedByIdMethod(abstractManager, pkeyProperties, simpleName)            addDeleteByIdMethod(abstractManager, pkeyProperties, name)            addDeleteEntryMethod(abstractManager, simpleName, pkeyProperties)            addStoreMethod(abstractManager, pkeyProperties, simpleName, properties)            for (index in indexes) {                indexCount++                addGetByIndexMethod(abstractManager, simpleName, name, properties, index)                addDeleteByIndexMethod(abstractManager, name, properties, index)            }            abstractManager.appendLine("}")            abstractManager.close()        }        private fun getIndexes(model: KSClassDeclaration): List<ExposedIndex> {            val preCode = Reflections.getCode(model)            val indexLines = preCode.lines().filter { it.contains("index(") }            val indexes = mutableListOf<ExposedIndex>()            for (index in indexLines) {                if (index.startsWith("//")) continue                val args = index.trim()                    .removeFirst("//.*".toRegex())                    .removeFirst("index(")                    .trim()                    .dropLast(1) // removes the last ")"                    .split("\\s*,\\s*".toRegex())                fun parseName(): String? {                    val nameOverride: String =                        "//\\s*name\\s*=\\s*(.*)".toRegex().find(index)?.groups?.get(1)?.value ?: ""                    return nameOverride.takeIf { it.isNotBlank() } ?: args.drop(0).firstOrNull { it.contains("\"") }                }                val name = parseName()                val argsNoName = args.filterNot { it == name }                val bool = argsNoName.firstOrNull { it == "true" || it == "false" }?.toBoolean() ?: false                val fields = argsNoName.filterNot { it == "true" || it == "false" }                indexes.add(ExposedIndex(bool, fields, name))            }            return indexes        }        private fun addStoreMethod(            abstractManager: OutputStream,            pkeyProperties: Sequence<KSPropertyDeclaration>,            simpleName: String,            properties: Sequence<KSPropertyDeclaration>        ) {            val propertyNames = properties.map { it.simpleName.asString() }            val pkeyNames = pkeyProperties.map { it.simpleName.asString() }            val propertyNoKeys = propertyNames.filterNot { pkeyNames.contains(it) }            val pkeyKeyPart = pkeyNames.joinToString(":") { "\${data.$it}" } // ${id1}:${id2}:...            @Language("kotlin")            val str = """     fun store(data: ${simpleName}Data) {        val key = "$classKeyPrefix:${pkeyKeyPart}"        scopedTransaction {            ${simpleName}.insertOrUpdate({${propertyNames.joinToString("\n") { " ".repeat(4*4) + "it[${simpleName}.${it}] = data.${it}" }}            }, {${propertyNoKeys.joinToString("\n") { " ".repeat(4*4) + "it[${simpleName}.${it}] = data.${it}" }}            })        }        val cachableStr = Json.encodeToString(data)        driverManager.setCacheEntry(key, cachableStr, 5)    }            """            abstractManager.appendLine(str)        }        private fun addGetCachedByIdMethod(            abstractManager: OutputStream,            pkeyProperties: Sequence<KSPropertyDeclaration>,            simpleName: String        ) {            val pKeyParams = getParametersFromProperties(pkeyProperties)            val pkeyNames = pkeyProperties.map { it.simpleName.asString() }            val pkeyKeyPart = pkeyNames.joinToString(":") { "\${$it}" } // ${id1}:${id2}:...            @Language("kotlin")            val code = """    suspend fun getCachedById($pKeyParams): $location.${simpleName}Data? {        val key = "$classKeyPrefix:${pkeyKeyPart}"        driverManager.getCacheEntry(key, 5)?.run {            if (this == "null") return null            return Json.decodeFromString<${simpleName}Data>(this)        }        val cachable = getById(${pkeyNames.joinToString(", ")})        val cachableStr = Json.encodeToString(cachable)        driverManager.setCacheEntry(key, cachableStr, 5)        return cachable    }            """            abstractManager.appendLine(code)        }        private fun addGetByIdMethod(            abstractManager: OutputStream,            pkeyProperties: Sequence<KSPropertyDeclaration>,            simpleName: String,            name: String,            properties: Sequence<KSPropertyDeclaration>        ) {            val pKeyParams = getParametersFromProperties(pkeyProperties)            abstractManager.appendLine("")            abstractManager.appendLine("    fun getById(${pKeyParams}): $location.${simpleName}Data? {")            abstractManager.appendLine("        return scopedTransaction {")            abstractManager.appendLine("             ${name}.select {")            abstractManager.appendLine("                 ${pkeyProperties.joinToString(".and") { "(${name}.$it.eq($it))" }}")            abstractManager.appendLine("             }.firstOrNull()?.let {")            abstractManager.appendLine("                 $location.${simpleName}Data.fromResRow(it)")            abstractManager.appendLine("             }")            abstractManager.appendLine("         }")            abstractManager.appendLine("    }")        }        private fun addDeleteByIdMethod(            abstractManager: OutputStream,            pkeyProperties: Sequence<KSPropertyDeclaration>,            name: String        ) {            val pKeyParams = getParametersFromProperties(pkeyProperties)            val pkeyNames = pkeyProperties.map { it.simpleName.asString() }            val pkeyKeyPart = pkeyNames.joinToString(":") { "\${$it}" } // ${id1}:${id2}:...            @Language("kotlin")            val str = """    fun deleteById(${pKeyParams}): Int {        val key = "$classKeyPrefix:${pkeyKeyPart}"        val res = scopedTransaction {             ${name}.deleteWhere {                 ${pkeyProperties.joinToString(".and") { "(${name}.$it.eq($it))" }}             }        }        driverManager.removeCacheEntry(key)        return res    }            """            abstractManager.appendLine(str)        }        private fun addDeleteEntryMethod(            abstractManager: OutputStream,            simpleName: String,            pkeyProperties: Sequence<KSPropertyDeclaration>,        ) {            @Language("kotlin")            val str = """    fun delete(data: $location.${simpleName}Data): Int {        return deleteById(${pkeyProperties.joinToString { "data.${it.simpleName.asString()}" }})    }            """            abstractManager.appendLine(str)        }        private fun addGetByIndexMethod(            abstractManager: OutputStream,            simpleName: String,            name: String,            properties: Sequence<KSPropertyDeclaration>,            index: ExposedIndex        ) {            val indexedProperties = properties.filter {                index.fields.contains(it.simpleName.asString())            }            val params = getParametersFromProperties(indexedProperties)            val returnType = if (index.unique) "$location.${simpleName}Data?" else                "List<$location.${simpleName}Data>"            val sanitizedName = index.name                ?.remove("\"")                ?.replace("_(.)".toRegex()) { res -> res.groupValues[1].uppercase() }                ?.replaceFirstChar { it.uppercase() } ?: "Index$indexCount"            abstractManager.appendLine("    // index fields: ${index.fields.joinToString()}")            abstractManager.appendLine("    // properties: ${properties.joinToString()}")            abstractManager.appendLine("    fun getBy${sanitizedName}(${params}): $returnType {")            abstractManager.appendLine("        return scopedTransaction {")            abstractManager.appendLine("             ${name}.select {")            abstractManager.appendLine("                 ${indexedProperties.joinToString(".and") { "(${name}.$it.eq($it))" }}")            if (index.unique) {                abstractManager.appendLine("             }.firstOrNull()?.let {")            } else {                abstractManager.appendLine("             }.map {")            }            abstractManager.appendLine("                 $location.${simpleName}Data.fromResRow(it)")            abstractManager.appendLine("             }")            abstractManager.appendLine("         }")            abstractManager.appendLine("    }")        }        private fun addDeleteByIndexMethod(            abstractManager: OutputStream,            name: String,            properties: Sequence<KSPropertyDeclaration>,            index: ExposedIndex        ) {            val indexedProperties = properties.filter {                index.fields.contains(it.simpleName.asString())            }            val params = getParametersFromProperties(indexedProperties)            val sanitizedName = index.name                ?.remove("\"")                ?.replace("_(.)".toRegex()) { res -> res.groupValues[1].uppercase() }                ?.replaceFirstChar { it.uppercase() } ?: "Index$indexCount"            abstractManager.appendLine("    // index fields: ${index.fields.joinToString()}")            abstractManager.appendLine("    // properties: ${properties.joinToString()}")            abstractManager.appendLine("    fun deleteBy${sanitizedName}(${params}): Int {")            abstractManager.appendLine("        return scopedTransaction {")            abstractManager.appendLine("             ${name}.deleteWhere {")            abstractManager.appendLine("                 ${indexedProperties.joinToString(".and") { "(${name}.$it.eq($it))" }}")            abstractManager.appendLine("             }")            abstractManager.appendLine("         }")            abstractManager.appendLine("    }")        }        /**         * @param properties Sequence<KSPropertyDeclaration>         * @return String: "name1: Type1, name2: Type2"         * **/        private fun getParametersFromProperties(properties: Sequence<KSPropertyDeclaration>) =            properties.joinToString(", ") {                it.simpleName.asString() + ": " + getType(it)            }        private fun getValueGetter(pd: KSPropertyDeclaration): String {            if (pd.type.resolve().innerArguments.firstOrNull()?.type?.resolve()?.declaration?.simpleName?.asString() == "EntityID") {                return ".value"            }            return ""        }        /**         * gets type from Column<[Any]> or Column<EntityID<[Any]>>         * @param pd variable declaration with Column or Column<EntityID> types         * @return package.ClassName         */        private fun getType(pd: KSPropertyDeclaration): String {            val innerColumnTypeRef = pd.type.resolve().innerArguments.firstOrNull()?.type            val innerColumnType = innerColumnTypeRef?.resolve()            return if (innerColumnType?.declaration?.simpleName?.asString() == "EntityID") {                val innerType = innerColumnTypeRef.resolve().innerArguments.firstOrNull()?.type                val type = innerType?.resolve()                val packageStr = type?.declaration?.packageName?.asString()?.let { "$it." } ?: ""                val nullableMarker = type?.isMarkedNullable?.let { if (it) "?" else "" }                packageStr + innerType.toString() + nullableMarker            } else {                val packageStr = innerColumnType?.declaration?.packageName?.asString()?.let { "$it." } ?: ""                val nullableMarker = innerColumnType?.isMarkedNullable?.let { if (it) "?" else "" }                packageStr + innerColumnTypeRef.toString() + nullableMarker            }        }        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {        }    }    data class ExposedIndex(        val unique: Boolean,        val fields: List<String>,        val name: String? = null,    )}