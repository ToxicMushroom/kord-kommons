package me.melijn.apredgres.tablemodel

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import me.melijn.apredgres.util.OPTION_PREFIX

internal class TableModelProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TableModelProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["${OPTION_PREFIX}_package"]!!,
            environment.options["${OPTION_PREFIX}_redis_key_prefix"]!!
        )
    }
}