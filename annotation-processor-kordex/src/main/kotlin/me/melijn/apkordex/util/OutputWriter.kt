package me.melijn.apkordex.util

import java.io.OutputStream

public fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

public fun OutputStream.appendLine(str: String) {
    this.write(str.toByteArray())
    this.write('\n'.code)
}