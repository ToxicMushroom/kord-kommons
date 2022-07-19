plugins {
    kotlin("jvm") version "1.7.10"
    id("maven-publish")
}

group = "me.melijn.kordkommons"
version = "0.1.0"

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    maven("https://maven.kotlindiscord.com/repository/maven-snapshots/")
    maven("https://maven.kotlindiscord.com/repository/maven-releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
}

val ksp = "1.7.10-1.0.6"
val kordEx = "1.5.5-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.google.devtools.ksp:symbol-processing-api:$ksp")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:$kordEx")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }
    withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
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
            this.artifactId = "apkordex"
            artifact(sourcesJar.get())
        }
    }
}