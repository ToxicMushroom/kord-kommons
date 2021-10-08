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
    implementation("org.slf4j:slf4j-api:1.7.32")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.21")
    testImplementation("ch.qos.logback:logback-classic:1.2.6")
}

tasks.test {
    useJUnitPlatform()
}