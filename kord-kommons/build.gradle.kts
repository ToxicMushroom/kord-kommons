import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.20"
    id("maven-publish")
}

group = "me.melijn.kordkommons"
version = "0.0.1"

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    maven("https://maven.kotlindiscord.com/repository/maven-snapshots/")
    maven("https://maven.kotlindiscord.com/repository/maven-releases/")
}

val kord = "0.8.0-M12"
val kordEx = "1.5.2-SNAPSHOT"
val kotlin = "1.6.20"
val kotlinX = "1.6.1" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core

dependencies {
    implementation("dev.kord:kord-core:$kord")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:$kordEx")
    implementation(project(":kommons"))

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin")
    testImplementation("ch.qos.logback:logback-classic:1.2.11")
}

tasks.test {
    useJUnitPlatform()
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }
    withType(KotlinCompile::class) {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://nexus.melijn.com/repository/maven-releases/")
            credentials {
                username = property("melijnPublisher").toString()
                password = property("melijnPassword").toString()
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifactId = "kord-kommons"
            artifact(sourcesJar.get())
        }
    }
}