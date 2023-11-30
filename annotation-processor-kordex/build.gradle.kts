plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
}

dependencies {
    implementation(libs.ksp)
    implementation(libs.koin.core)
    implementation(libs.kotlin.stdlib)

    api(libs.kordex)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.8"
    freeCompilerArgs = listOf("-Xskip-prerelease-check")
}