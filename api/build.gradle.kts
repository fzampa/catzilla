import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    alias(libs.plugins.errorprone)
}

group = "app.catzilla"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation(libs.springdoc.webmvc.ui)
    implementation(libs.bouncycastle.bcprov)
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    errorprone(libs.errorprone.core)

    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.errorprone {
        disableWarningsInGeneratedCode = true
        // Fail the build on Error Prone warnings in main code; keep tests lenient.
        allErrorsAsWarnings = name.contains("Test")
    }
    if (!name.contains("Test")) {
        options.compilerArgs.add("-Werror")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Lets the contract test find ../contracts/openapi.yaml and optionally rewrite it.
    systemProperty("contract.file", rootProject.file("../contracts/openapi.yaml").absolutePath)
    finalizedBy(tasks.jacocoTestReport)
}

tasks.bootJar {
    archiveFileName = "catzilla-api.jar"
}

tasks.jar {
    enabled = false
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

spotless {
    java {
        palantirJavaFormat(
            libs.versions.palantir.java.format
                .get(),
        )
        removeUnusedImports()
        importOrder()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

// Regenerates contracts/openapi.yaml from the running application's springdoc output.
tasks.register<Test>("updateContract") {
    description = "Rewrites ../contracts/openapi.yaml from the current API."
    group = "verification"
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("*OpenApiContractTest") }
    systemProperty("contract.update", "true")
}
