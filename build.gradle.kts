
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktlintVersion = "1.8.0"
val logstashVersion = "9.0"
val verapdfVersion = "1.30.2"
val mockkVersion = "1.14.11"
val springMockkVersion = "5.0.1"
val tokenValidationVersion = "6.0.12"
val simpleSlackPosterVersion = "1.0.0"
val kodeverkVersion = "3.3.10"

plugins {
    val kotlinVersion = "2.4.10"
    id("org.springframework.boot") version "4.1.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("dev.detekt") version "2.0.0-alpha.6"
    idea
}

apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

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
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
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

val ktlintCli = configurations.create("ktlintCli")

dependencies {
    ktlintCli("com.pinterest.ktlint:ktlint-cli:$ktlintVersion")
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

val ktlintInputs =
    files(
        "src/main/kotlin",
        "src/test/kotlin",
        "build.gradle.kts",
        "settings.gradle.kts",
    )

fun registerKtlintTask(
    name: String,
    format: Boolean,
) {
    tasks.register<JavaExec>(name) {
        group = "verification"
        description =
            if (format) {
                "Formats Kotlin sources with ktlint"
            } else {
                "Checks Kotlin sources with ktlint"
            }
        classpath = ktlintCli
        mainClass.set("com.pinterest.ktlint.Main")
        args =
            buildList {
                if (format) {
                    add("--format")
                }
                addAll(ktlintInputs.files.map { it.relativeTo(rootDir).path })
            }
        inputs
            .files(ktlintInputs)
            .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    }
}

registerKtlintTask("ktlintCheck", format = false)
registerKtlintTask("ktlintFormat", format = true)

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(false)
}

// NamedArguments reports only with analysis tasks that have compile classpath.
tasks.named("detekt") {
    enabled = false
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget.set(JvmTarget.JVM_21.target)
    reports {
        html.required.set(true)
        checkstyle.required.set(true)
        sarif.required.set(false)
        markdown.required.set(false)
    }
}

tasks.named("check") {
    dependsOn("ktlintCheck")
    dependsOn("detektMain", "detektTest")
}

java.sourceCompatibility = JavaVersion.VERSION_21

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xannotation-default-target=param-property")
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
