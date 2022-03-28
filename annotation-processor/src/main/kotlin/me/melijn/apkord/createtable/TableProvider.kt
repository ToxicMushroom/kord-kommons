package me.melijn.apkord.createtable

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class TableProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TableProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["apkord_package"]!!
        )
    }
}