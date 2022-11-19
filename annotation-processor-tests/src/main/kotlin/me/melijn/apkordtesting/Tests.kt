package me.melijn.apkordtesting

import kotlinx.coroutines.delay
import me.melijn.ap.injector.Inject
import me.melijn.apkordtesting.events.ReadyListener
import me.melijn.gen.Settings
import me.melijn.kordkommons.logger.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Inject(true)
class Tests : KoinComponent {

    val settings by inject<Settings>()
    val log by Log

    suspend fun tests() {
        delay(2000)
        val readyListener by inject<ReadyListener>()
        val duration = System.currentTimeMillis() - readyListener.time
        require(duration > 1000) {"stinky, readyListener was not initialized 2 seconds ago"}
        log.info { "Tests successful!" }
    }
}
