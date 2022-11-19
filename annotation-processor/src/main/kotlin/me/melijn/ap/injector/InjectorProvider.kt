package me.melijn.ap.injector

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import me.melijn.ap.util.OPTION_PREFIX

internal class InjectorProvider : SymbolProcessorProvider {
    private val defaultInitPlaceholder = """
        val %varName% by inject<%className%>(%className%::class.java)
        %varName%.toString()

    """.trimIndent()

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return InjectorProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["${OPTION_PREFIX}_package"]!!,
            environment.options["${OPTION_PREFIX}_imports"] ?: "",
            environment.options["${OPTION_PREFIX}_interfaces"] ?: "",
            environment.options["${OPTION_PREFIX}_init_placeholder"] ?: defaultInitPlaceholder
        )
    }
}
