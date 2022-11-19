package me.melijn.ap.injector

import org.koin.core.module.Module

public abstract class InjectorInterface {

    public abstract val module: Module

    public abstract fun initInjects()

}