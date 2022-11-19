package me.melijn.apredgres.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

public object Reflections {

    public fun getCode(clazz: KSClassDeclaration): String {
        val field = clazz.javaClass.getDeclaredField("descriptor\$delegate")
        field.isAccessible = true
        val lazyDescriptor = field.get(clazz)
        val lazyValueMethod = Lazy::class.java.getMethod("getValue")

        val lazyValue = lazyValueMethod.invoke(lazyDescriptor)
        val declarationProvider = lazyValue.javaClass.getDeclaredField("declarationProvider")
        declarationProvider.isAccessible = true
        return declarationProvider.get(lazyValue).toString()
    }

}