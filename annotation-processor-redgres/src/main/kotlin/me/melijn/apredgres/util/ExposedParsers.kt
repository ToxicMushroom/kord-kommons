package me.melijn.apredgres.util

import com.google.devtools.ksp.innerArguments
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

public object ExposedParsers {
    public fun getValueGetter(pd: KSPropertyDeclaration): String {
        if (pd.type.resolve().innerArguments.firstOrNull()?.type?.resolve()?.declaration?.simpleName?.asString() == "EntityID") {
            return ".value"
        }
        return ""
    }
}