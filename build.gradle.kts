import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val exposedVersion = "0.21.1"

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("org.jetbrains.kotlin.plugin.spring") version "1.3.72"
    idea
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.projectreactor:reactor-spring:1.0.1.RELEASE")

    implementation("com.zaxxer:HikariCP")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.kafka:spring-kafka")
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src/kotlin")
kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")

sourceSets["main"].resources.srcDirs("src/resources")
sourceSets["test"].resources.srcDirs("test/resources")
