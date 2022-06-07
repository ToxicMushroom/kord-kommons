plugins {
    kotlin("jvm") version "1.6.20"
    id("com.google.devtools.ksp") version "1.6.20-1.0.4"
    kotlin("plugin.serialization") version "1.6.20"
}

group = "me.melijn.kordkommons"
version = "0.0.2"

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

val kotlin = "1.6.20"
val kotlinX = "1.6.1"
val ksp = "1.6.20-1.0.4"
val koin = "3.1.5"
val kordKommons = "1.2.3"
val apKordVersion = "0.1.3"
val redgresKommons = "0.0.3"

dependencies {
    implementation(kotlin("stdlib"))
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinX")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinX")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-jdk8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinX")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation("com.google.devtools.ksp:symbol-processing-api:$ksp")
    implementation("io.insert-koin:koin-core:$koin")

    val apKord = project(":annotation-processor")
    implementation(apKord)
    ksp(apKord)
//    val apKordex = project(":annotation-processor-kordex")
//    implementation(apKordex)
//    ksp(apKordex)

    implementation("org.jetbrains.exposed:exposed-core:0.37.3")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.37.3")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:0.37.3")

    // https://search.maven.org/artifact/com.zaxxer/HikariCP
    implementation("com.zaxxer:HikariCP:5.0.1")

    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.3.3")

    // https://mvnrepository.com/artifact/io.lettuce/lettuce-core
    implementation("io.lettuce:lettuce-core:6.1.8.RELEASE")

    // https://github.com/cdimascio/dotenv-kotlin
    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.2.11")

    implementation(project(":kommons"))
    implementation(project(":redgres-kommons"))
}

ksp {
    arg("apkordex_package", "me.melijn.gen")
    arg("ap_package", "me.melijn.gen")
    arg("ap_redis_key_prefix", "kommonstests:")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
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