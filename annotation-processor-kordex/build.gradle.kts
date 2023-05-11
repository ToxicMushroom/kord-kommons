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
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.6"
    freeCompilerArgs = listOf("-Xskip-prerelease-check")
}