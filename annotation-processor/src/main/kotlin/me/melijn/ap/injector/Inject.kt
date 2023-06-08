package me.melijn.ap.injector

public annotation class Inject(
    val init: Boolean = false,
    val initGroup: Int = 0
)
