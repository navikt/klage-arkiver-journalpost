import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val gcsVersion = "1.108.0"

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

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.google.cloud:google-cloud-storage:$gcsVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "org.junit.vintage")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
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

tasks.test {
    useJUnitPlatform()
}

kotlin.sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
kotlin.sourceSets["test"].kotlin.srcDirs("src/test/kotlin")

sourceSets["main"].resources.srcDirs("src/main/resources")
sourceSets["test"].resources.srcDirs("src/test/resources")
