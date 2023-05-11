import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
    `tested-module`
}

dependencies {
    api(libs.kordex)

    api(project(":kommons"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.logback)
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.6"
}

dokkaModule {
    includes.add("packages.md")
}