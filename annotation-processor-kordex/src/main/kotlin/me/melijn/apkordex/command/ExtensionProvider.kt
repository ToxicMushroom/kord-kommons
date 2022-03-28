package me.melijn.apkordex.command

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.kotlindiscord.kord.extensions.utils.env

class ExtensionProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ExtensionProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["apkordex_package"]!!
        )
    }
}
