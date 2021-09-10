plugins {
    kotlin("jvm") version "1.5.30"
}

group = "me.melijn"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.21")
}

tasks.test {
    useJUnitPlatform()
}