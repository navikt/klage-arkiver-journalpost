import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logstashVersion = "8.1"
val verapdfVersion = "1.28.2"
val mockkVersion = "1.14.6"
val springMockkVersion = "4.0.2"
val tokenValidationVersion = "5.0.37"
val simpleSlackPosterVersion = "1.0.0"
val kodeverkVersion = "1.11.11"

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

plugins {
    val kotlinVersion = "2.2.20"
    id("org.springframework.boot") version "3.5.6"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    idea
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-aspects")
    implementation("org.projectreactor:reactor-spring:1.0.1.RELEASE")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("no.nav.security:token-client-spring:$tokenValidationVersion")
    implementation("no.nav.security:token-validation-spring:$tokenValidationVersion")

    implementation("no.nav.slackposter:simple-slack-poster:$simpleSlackPosterVersion")
    implementation("no.nav.klage:klage-kodeverk:$kodeverkVersion")

    implementation("org.verapdf:validation-model:$verapdfVersion") {
        exclude(group = "com.sun.xml.bind")
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "org.junit.vintage")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

java.sourceCompatibility = JavaVersion.VERSION_21

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}
