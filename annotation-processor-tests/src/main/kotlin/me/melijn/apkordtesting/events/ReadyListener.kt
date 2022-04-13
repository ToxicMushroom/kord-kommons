package me.melijn.apkordtesting.events

import me.melijn.ap.injector.Inject

@Inject(true)
class ReadyListener {

    val time = System.currentTimeMillis()

}