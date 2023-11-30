plugins {
    `kordex-module`
    `published-module`
    `dokka-module`
}

dependencies {
    implementation(libs.ksp)
    implementation(libs.kotlin.stdlib)
    api(project(":kommons"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

compileKotlin.kotlinOptions {
    languageVersion = "1.8"
}