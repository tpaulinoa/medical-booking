import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.exercise"
version = "0.1.0-SNAPSHOT"
description = "Medical appointment scheduling service"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
}

repositories {
    mavenCentral()
}

// Not in the Spring Boot BOM. The 2.9 line is the one for Spring Boot 3.
val springdocVersion = "2.9.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.awaitility:awaitility")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-parameters"))
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<BootRun>("bootRunDev") {
    group = "application"
    description = "Runs the application with the dev profile, which also loads the fictional seed data."
    dependsOn("dbUp")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(tasks.named<BootRun>("bootRun").flatMap { it.mainClass })
    systemProperty("spring.profiles.active", "dev")
}

tasks.register<Exec>("dbUp") {
    group = "application"
    description = "Starts Postgres, Kafka and Mailpit and waits for all three to be healthy."
    commandLine("docker", "compose", "up", "-d", "--wait", "postgres", "kafka", "mailpit")
}

// Deletes the database volume, e.g. after editing a migration that was already applied.
tasks.register<Exec>("dbReset") {
    group = "application"
    description = "Destroys the Postgres volume so the next run applies every migration from scratch."
    commandLine("docker", "compose", "down", "-v")
    finalizedBy("dbUp")
}

jacoco {
    toolVersion = "0.8.12"
}

val coverageExclusions = listOf(
    "**/api/**",
    "**/config/**",
    "**/*Application*"
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } })
    )
}

tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(coverageExclusions) } })
    )
    violationRules {
        rule { limit { minimum = "0.70".toBigDecimal() } }
    }
}

tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
