import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    kotlin("jvm")
}

kotlin {
    // We still need to set this, because the IntelliJ Kotlin plugin Inspections
    // look for this option instead of the CLI arg
    explicitApi = ExplicitApiMode.Disabled
}