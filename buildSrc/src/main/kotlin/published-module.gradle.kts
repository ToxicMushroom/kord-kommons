import org.gradle.api.publish.maven.MavenPublication

plugins {
    `maven-publish`
}

val sourceJar: Task by tasks.getting
val javadocJar: Task by tasks.getting

publishing {
    repositories {
        maven {
            name = "Melijn"

            url = if (project.version.toString().contains("SNAPSHOT")) {
                uri("https://reposilite.melijn.com/snapshots/")
            } else {
                uri("https://reposilite.melijn.com/releases/")
            }

            credentials {
                username = project.findProperty("melijnReposilitePub") as String? ?: System.getenv("KOTLIN_DISCORD_USER")
                password = project.findProperty("melijnReposilitePassword") as String?
                    ?: System.getenv("KOTLIN_DISCORD_PASSWORD")
            }

            version = project.version
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ToxicMushroom/kord-kommons")
            credentials {
                username = project.findProperty("ghpMkMavenUser") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("ghpMkMavenPat") as String? ?: System.getenv("TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components.getByName("java"))

            artifact(sourceJar)
            artifact(javadocJar)
        }
    }
}
