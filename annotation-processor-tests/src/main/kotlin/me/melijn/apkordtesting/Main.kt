package me.melijn.apkordtesting

import me.melijn.ap.createtable.CreateTableInterface
import me.melijn.ap.injector.InjectorInterface
import me.melijn.gen.Settings
import me.melijn.kordkommons.database.ConfigUtil
import me.melijn.kordkommons.database.DriverManager
import me.melijn.kordkommons.redis.RedisConfig
import me.melijn.kordkommons.utils.ReflectUtil
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject

val koin = startKoin {

}

suspend fun main() {
    koin.modules(module {
        single { Settings } bind Settings::class
    })
    val settings by inject<Settings>(Settings::class.java)
    val driverManager = initDriverManager(settings)
    koin.modules(module {
        single { driverManager } bind DriverManager::class
    })

    val injectorInterface = ReflectUtil.getInstanceOfKspClass<InjectorInterface>(
        "me.melijn.gen", "InjectionKoinModule"
    )
    GlobalContext.loadKoinModules(injectorInterface.module)
    injectorInterface.initInjects()

    val readyListener by inject<Tests>(Tests::class.java)
    readyListener.tests()
}

fun initDriverManager(settings: Settings): DriverManager {
    val redisConfig = settings.redis.run { RedisConfig(enabled, host, port, user, pass) }
    val hikariConfig = settings.database.run {
        ConfigUtil.generateDefaultHikariConfig(host, port, name, user, pass)
    }

    val createTableInterface = ReflectUtil.getInstanceOfKspClass<CreateTableInterface>(
        "me.melijn.gen", "CreateTablesModule"
    )
    return DriverManager(hikariConfig, redisConfig) { createTableInterface.createTables() }
}