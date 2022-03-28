package me.melijn.apkord.injector

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class InjectorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return InjectorProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["apkord_package"]!!
        )
    }
}
