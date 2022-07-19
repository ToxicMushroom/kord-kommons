### **this project is still in WIP, changes may be breaking**

All artifacts can be found on https://nexus.melijn.com/#browse/browse:maven-releases:me%2Fmelijn%2Fkordkommons

## Module info
- _redgres-kommons_ is a redis-postgres utility library, it depends on Exposed, hikariCP, postgresql and lettuce
- _kommons_ is for discord bot and java utilities
- _kord-kommons_ is for kord and kord-ex utilities
- _ap_ is an annotation processor using KSP
- _ap-kordex_ is an annotation processor using KSP and depends on kordex
 

**Examples:** 
 - [loading koin injection](README.md#loading-koin-injection)
 - [initalizing redgres driverManager](README.md#initializing-redgres-drivermanager)
 - [using settings template](README.md#using-settings-template)

**More example usage can be found here:** [melijn-bot](https://github.com/Melijn/melijn-bot/)

## Gradle
```kt
repositories {
    maven("https://nexus.melijn.com/repository/maven-releases/")
}

// modules can be added individually
dependencies {
    implementation("me.melijn.kordkommons:kommons:LATEST") // see nexus above for version
    implementation("me.melijn.kordkommons:kord-kommons:LATEST")
    implementation("me.melijn.kordkommons:redgres-kommons:LATEST")
}
```
**Gradle annotation processing**
```kt
plugins {
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

dependencies {
    val apKord = "me.melijn.kordkommons:ap:LATEST"
    val apKordex = "me.melijn.kordkommons:apkordex:LATEST"
    implementation(apKord)
    implementation(apKordex)
    ksp(apKord)
    ksp(apKordex)
}

ksp {
    // these are arguments for ksp and configurable but required
    arg("apkordex_package", "me.melijn.gen")
    arg("ap_package", "me.melijn.gen")
    arg("ap_redis_key_prefix", "melijn:")
}
```

## Examples
### loading koin injection
```kt
val injectorInterface = ReflectUtil.getInstanceOfKspClass<InjectorInterface>(
    "me.melijn.gen", "InjectionKoinModule"
)
loadKoinModules(injectorInterface.module)
```

### Initializing redgres driverManager
```kt
    val settings // your settings object
    val redisConfig = settings.redis.run { RedisConfig(enabled, host, port, user, pass) }
    val hikariConfig = settings.database.run {
        ConfigUtil.generateDefaultHikariConfig(host, port, name, user, pass)
    }

    val createTableInterface = ReflectUtil.getInstanceOfKspClass<CreateTableInterface>(
        "me.melijn.gen", "CreateTablesModule"
    )
    val driverManager = DriverManager(hikariConfig, redisConfig) { createTableInterface.createTables() }

```

### Using settings template
```kt
@SettingsTemplate("import me.melijn.bot.model.Environment")
private class Template {

    class Database : BotSettings("db") {
        /** postgres://user:pass@host:port/name **/
        val host by string("host", "localhost")
        val port by int("port", 5432)
        val user by string("user", "postgres")
        val pass by string("pass")
        val name by string("name", "melijn-bot") // db name
    }

    class Redis : BotSettings("redis") {
        val enabled by boolean("enabled", true)
        val host by string("host", "localhost")
        val port by int("port", 6379)
        val user by stringN("user")
        val pass by string("pass")
    }
    
    class Api : BotSettings("api") {
        
        // nesting support
        class Discord : BotSettings("discord") {
            val token by string("token")
            val gateway by string("gateway", "")
        }
    }
}
```