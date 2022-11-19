import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()

        maven {
            name = "Sonatype Snapshots"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }

        maven {
            name = "MelijnRepo Snapshots"
            url = uri("https://reposilite.melijn.com/releases/")
        }
        maven {
            name = "MelijnRepo Releases"
            url = uri("https://reposilite.melijn.com/releases/")
        }
    }
}

plugins {
    `maven-publish`

    kotlin("jvm")
}

val projectVersion: String by project

group = "me.melijn.kommons"
version = projectVersion

val printVersion = task("printVersion") {
    doLast {
        print(version.toString())
    }
}

repositories {
    google()
    mavenCentral()

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        name = "MelijnRepo Snapshots"
        url = uri("https://reposilite.melijn.com/snapshots/")
    }

    maven {
        name = "MelijnRepo Releases"
        url = uri("https://reposilite.melijn.com/releases")
    }
}

subprojects {
    group = "me.melijn.kommons"
    version = projectVersion

    tasks.withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.contracts.ExperimentalContracts"
    }

    repositories {
        rootProject.repositories.forEach {
            if (it is MavenArtifactRepository) {
                maven {
                    name = it.name
                    url = it.url
                }
            }
        }
    }
}
