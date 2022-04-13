package me.melijn.apkordtesting

import kotlinx.coroutines.delay
import me.melijn.ap.injector.Inject
import me.melijn.apkordtesting.events.ReadyListener
import me.melijn.gen.Settings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Inject(true)
class Tests : KoinComponent {

    val settings by inject<Settings>()

    suspend fun tests() {
        delay(2000)
        val readyListener by inject<ReadyListener>()
        val duration = System.currentTimeMillis() - readyListener.time
        assert(duration > 1000)
    }

}
