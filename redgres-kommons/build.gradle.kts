import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
    `tested-module`
}

dependencies {
    implementation(libs.exposed.core)
    implementation(libs.exposed.springbs)

    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.lettuce)

    implementation(libs.kx.coroutines.core)
    implementation(libs.kx.coroutines.jdk)

    api(project(":kommons"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.logback)
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.8"
}

dokkaModule {
    includes.add("packages.md")
}