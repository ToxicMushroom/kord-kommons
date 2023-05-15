package me.melijn.apredgres.util

import com.google.devtools.ksp.innerArguments
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import me.melijn.apredgres.ExposedIndex
import me.melijn.kordkommons.utils.remove
import me.melijn.kordkommons.utils.removeFirst

public object Reflections {

    /** @return source code of the [KSClassDeclaration] as String */
    public fun getCode(clazz: KSClassDeclaration): String {
        val field = clazz.javaClass.getDeclaredField("descriptor\$delegate")
        field.isAccessible = true
        val lazyDescriptor = field.get(clazz)
        val lazyValueMethod = Lazy::class.java.getMethod("getValue")

        val lazyValue = lazyValueMethod.invoke(lazyDescriptor)
        val declarationProvider = lazyValue.javaClass.getDeclaredField("declarationProvider")
        declarationProvider.isAccessible = true
        return declarationProvider.get(lazyValue).toString()
    }


    /** @return field names from a PrimaryKey [KSPropertyDeclaration] */
    public fun getFields(pkeyProperty: KSPropertyDeclaration): MutableList<String> {
        /** janky hack part, don't touch unless it broke pls **/
        val field = pkeyProperty.javaClass.getDeclaredField("propertyDescriptor\$delegate")
        field.isAccessible = true
        val lazyPropertyDesciptor = field.get(pkeyProperty)
        val lazyValueMethod = Lazy::class.java.getMethod("getValue")

        val lazyValue = lazyValueMethod.invoke(lazyPropertyDesciptor)

        val propertyDescriptor = lazyValue
        val aaaa = lazyValue::class.java.getMethod("getSource").invoke(propertyDescriptor)
        val fish = aaaa::class.java.getMethod("getPsi").invoke(aaaa)
        val lastChildField = fish::class.java.getMethod("getLastChild")
        val firstChildField = fish::class.java.getMethod("getFirstChild")
        val last1 = lastChildField.invoke(fish)
        val last2 = lastChildField.invoke(last1)
        var invokeEl = firstChildField.invoke(last2)

        val fieldList = mutableListOf<String>()
        while (invokeEl != null) {
            val nextSiblingField = invokeEl::class.java.getMethod("getNextSibling")
            val type = invokeEl.toString()
            if (type == "VALUE_ARGUMENT") {
                val first2 = firstChildField.invoke(invokeEl)
                val first3 = firstChildField.invoke(first2)
                val text = first3::class.java.getMethod("getText").invoke(first3)
                fieldList.add(text.toString())
            }
            invokeEl = try {
                nextSiblingField.invoke(invokeEl)
            } catch (t: Throwable) {
                null
            }
        }
        return fieldList
    }

    /** @return fieldNames that are autoIncrementing */
    public fun getAutoIncrementing(model: KSClassDeclaration): List<String> {
        val code = getCode(model)
        val results = "\\.autoIncrement\\(".toRegex().findAll(code).toList()
        val propPattern = "va[lr]\\s+(\\w+)".toRegex()
        var prev = 0
        val props = mutableListOf<String>()
        for (res in results) {
            val searchFrom = res.range.first

            val prop = propPattern.findAll(code.substring(prev, searchFrom)).lastOrNull() ?: run {
                throw IllegalStateException("Found .autoIncrement( but no property before it")
            }
            props.add(prop.groups[1]!!.value)
            prev = searchFrom
        }
        return props
    }

    /**
     * Can currently handle
     *  - index("name", field1, field2)
     *  - index(unique = true, field1, field2) // name = field1_field2
     *  - .index("name", field1, field2)
     *  - val field = long("name").index("index_name")
     *  And should be able to handle their combinations
     * @return All found exposed indices in a Table
     */
    public fun getIndexes(model: KSClassDeclaration): List<ExposedIndex> {
        val preCode = getCode(model)
        val indexLines = preCode.lines().filter { it.contains("index(") }

        val indexes = mutableListOf<ExposedIndex>()
        for (index in indexLines) {
            if (index.startsWith("//")) continue
            val args = index.trim()
                .removeFirst("//.*".toRegex())
                .removeFirst("index(")
                .removeFirst("(.*)?\\.index\\(".toRegex())
                .trim()
                .dropLast(1) // removes the last ")"
                .split("\\s*,\\s*".toRegex())

            fun parseName(): String? {
                val nameOverride: String =
                    "//\\s*name\\s*=\\s*(.*)".toRegex().find(index)?.groups?.get(1)?.value ?: ""
                return nameOverride.takeIf { it.isNotBlank() } ?: args.firstOrNull { it.contains("\"") }
            }

            val name = parseName()
            val argsNoName = args.filterNot { it == name }
            val bool = argsNoName.firstOrNull { it == "true" || it == "false" }?.toBoolean() ?: false
            val fields = argsNoName.filterNot { it == "true" || it == "false" }

            indexes.add(ExposedIndex(bool, fields, name))
        }
        return indexes
    }

    /**
     * Will try to get an UpperCamelCase index name, [number] is used when the index didn't provide a name
     *
     * - "guild_user" -> "GuildUser"
     * - "\"guild_user\"" -> "GuildUser"
     * - `null` -> "Index$[number]"
     */
    public fun getSanitizedNameFromIndex(index: ExposedIndex, number: Int): String = index.name
        ?.remove("\"")
        ?.replace("_(.)".toRegex()) { res -> res.groupValues[1].uppercase() }
        ?.replaceFirstChar { it.uppercase() } ?: "Index$number"

    /**
     * @param properties Sequence<KSPropertyDeclaration>
     * @return String: "name1: Type1, name2: Type2"
     */
    public fun getParametersFromProperties(properties: Sequence<KSPropertyDeclaration>): String =
        properties.joinToString(", ") {
            it.simpleName.asString() + ": " + getType(it, true)
        }


    /**
     * gets type from Column<[Any]> or Column<EntityID<[Any]>>
     * @param pd variable declaration with Column or Column<EntityID> types
     * @return package.ClassName
     */
    public fun getType(
        pd: KSPropertyDeclaration,
        simple: Boolean = false,
        removeNullable: Boolean = false
    ): String {
        val innerType =
            pd.type.resolve().innerArguments.firstOrNull()?.type?.resolve() ?: return "ERROR(couldn't get type)"

        return if (innerType.declaration.simpleName.asString() == "EntityID") {
            // Column<EntityID<InnerType>>
            val innerInnerType = innerType.innerArguments.firstOrNull()?.type?.resolve()
                ?: return "ERROR(couldn't get type)"
            typeAndTypeRefIntoQualifiedName(innerInnerType, simple, removeNullable)
        } else {
            // Column<InnerType>
            typeAndTypeRefIntoQualifiedName(innerType, simple, removeNullable)
        }
    }

    /**
     * Create fully qualified name for a given [KSType]
     *
     * @param removeNullable wether to remove the ? from the [type] or not
     * @return "package.ClassName" when [simple] = false or "ClassName" when [simple] = true
     */
    private fun typeAndTypeRefIntoQualifiedName(
        type: KSType,
        simple: Boolean,
        removeNullable: Boolean
    ): String {
        var typeName = type.toString()
        if (removeNullable) typeName = type.toString().dropLastWhile { '?' == it }
        return if (simple) {
            typeName
        } else {
            val packagePrefix = type.declaration.packageName.asString().let { "$it." }
            packagePrefix + typeName
        }
    }

}