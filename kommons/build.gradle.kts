import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
    `tested-module`
}

dependencies {
    implementation(libs.logging)
    implementation(libs.kotlin.stdlib)

    implementation(libs.dotenv)

    implementation(libs.kx.datetime)
    implementation(libs.kx.coroutines.core)
    implementation(libs.kx.coroutines.jdk)
    implementation(libs.kx.ser)

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