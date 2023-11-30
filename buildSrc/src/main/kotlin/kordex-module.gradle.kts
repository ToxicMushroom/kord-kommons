import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val sourceJar = task("sourceJar", Jar::class) {
    dependsOn(tasks["classes"])
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar = task("javadocJar", Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
    from(tasks.javadoc)
}

tasks {
    build {
        finalizedBy(sourceJar, javadocJar)
    }

    kotlin {
        explicitApi()
    }

    jar {
        from(rootProject.file("build/LICENSE-kordex"))
    }

    afterEvaluate {
        tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = "21"
            targetCompatibility = "21"
        }

        withType<KotlinCompile>().configureEach {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
}