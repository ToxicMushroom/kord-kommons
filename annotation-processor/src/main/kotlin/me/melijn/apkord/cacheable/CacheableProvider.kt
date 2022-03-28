package me.melijn.apkord.cacheable

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class CacheableProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return CacheableProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options["apkord_package"]!!,
            environment.options["redisKeyPrefix"]!!
        )
    }
}