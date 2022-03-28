package me.melijn.ap.settings

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import me.melijn.ap.util.OPTION_PREFIX

class SettingsProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SettingsProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["${OPTION_PREFIX}_package"]!!
        )
    }
}