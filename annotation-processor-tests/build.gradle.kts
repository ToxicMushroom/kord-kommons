plugins {
    application
    `kordex-module`
    `dokka-module`
    `ksp-module`
    `disable-explicit-api-mode`
}

buildscript {
    repositories {
        maven {
            name = "Melijn Nexus"
            url = uri("https://nexus.melijn.com/repository/maven-public/")
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)

    implementation(libs.kx.coroutines.core)
    implementation(libs.kx.coroutines.jdk)

    implementation(libs.kx.ser)

    implementation(libs.ksp)
    implementation(libs.koin.core)

    val apKord = project(":annotation-processor")
    val apRedgres = project(":annotation-processor-redgres")
    val apKordex = project(":annotation-processor-kordex")
    implementation(apKord)
    ksp(apKord)
    implementation(apRedgres)
    ksp(apRedgres)
    implementation(apKordex)
    ksp(apKordex)

    implementation(libs.kordex)

    implementation(libs.exposed.core)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.springbs)

    implementation(libs.hikaricp) // https://search.maven.org/artifact/com.zaxxer/HikariCP
    implementation(libs.postgresql) // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation(libs.lettuce) // https://mvnrepository.com/artifact/io.lettuce/lettuce-core

    implementation(libs.dotenv) // https://github.com/cdimascio/dotenv-kotlin
    implementation(libs.logging) // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic

    implementation(project(":kommons"))
    implementation(project(":redgres-kommons"))
}

ksp {
    arg("ap_kordex_package", "me.melijn.gen")
    arg("ap_redgres_package", "me.melijn.gen")
    arg("ap_redgres_redis_key_prefix", "melijn:")
    arg("ap_package", "me.melijn.gen")
    arg("ap_imports", "import org.koin.core.context.GlobalContext;import com.kotlindiscord.kord.extensions.utils.getKoin;import org.koin.core.parameter.ParametersHolder;")
    arg("ap_interfaces", "")
    arg("ap_init_placeholder", "GlobalContext.get().get<%className%> { ParametersHolder() }")
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.6"
}

application {
    this.mainClass.set("me.melijn.apkordteseting.MainKt")
}
