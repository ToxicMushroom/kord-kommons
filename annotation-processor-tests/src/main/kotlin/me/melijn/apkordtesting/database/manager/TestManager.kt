package me.melijn.apkordtesting.database.manager

import me.melijn.ap.injector.Inject
import me.melijn.gen.database.manager.AbstractTestModelManager
import me.melijn.kordkommons.database.DriverManager

@Inject
class TestManager(override val driverManager: DriverManager) : AbstractTestModelManager(driverManager) {
}