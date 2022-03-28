package me.melijn.apkord.settings

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class SettingsProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SettingsProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["apkord_package"]!!
        )
    }
}