package me.melijn.ap.cacheable

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import me.melijn.ap.util.OPTION_PREFIX

class CacheableProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {

        return CacheableProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["${OPTION_PREFIX}_package"]!!,
            environment.options["${OPTION_PREFIX}_redis_key_prefix"]!!
        )
    }
}