package me.melijn.apkordtesting

import me.melijn.gen.Settings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Tests : KoinComponent {

    val settings by inject<Settings>()


}
