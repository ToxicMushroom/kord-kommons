package me.melijn.apkordex.command

import com.kotlindiscord.kord.extensions.extensions.Extension

abstract class ExtensionInterface {
    abstract val list: List<Extension>
}