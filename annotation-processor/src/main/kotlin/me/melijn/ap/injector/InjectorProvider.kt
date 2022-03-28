package me.melijn.ap.injector

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import me.melijn.ap.util.OPTION_PREFIX

class InjectorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return InjectorProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["${OPTION_PREFIX}_package"]!!
        )
    }
}
