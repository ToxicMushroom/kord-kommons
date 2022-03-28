package me.melijn.apkord.injector

import org.koin.core.module.Module

abstract class InjectorInterface {
    abstract val module: Module
}