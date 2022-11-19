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
    languageVersion = "1.6"
}

dokkaModule {
    includes.add("packages.md")
}
//dependencies {
//    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
//    implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")
//    implementation("org.slf4j:slf4j-api:1.7.36")
//
//    // Coroutine utils
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinX")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinX")
//
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
//    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3")
//
//    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin")
//    testImplementation("ch.qos.logback:logback-classic:1.2.11")
//}