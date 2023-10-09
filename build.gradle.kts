import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logstashVersion = "7.4"
val resilience4jVersion = "2.1.0"
val verapdfVersion = "1.24.1"
val mockkVersion = "1.13.8"
val springMockkVersion = "4.0.2"
val tokenValidationVersion = "3.1.7"
val simpleSlackPosterVersion = "0.1.8"
val kodeverkVersion = "1.6.10"

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

plugins {
    val kotlinVersion = "1.9.10"
    id("org.springframework.boot") version "3.1.4"
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
    implementation("org.projectreactor:reactor-spring:1.0.1.RELEASE")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("no.nav.security:token-client-spring:$tokenValidationVersion")
    implementation("no.nav.security:token-validation-spring:$tokenValidationVersion")

    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")

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

java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
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
