plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    alias(libs.plugins.openapi.generator)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val openapiGenDir = layout.buildDirectory.dir("generated/openapi")
val openapiOpsGenDir = layout.buildDirectory.dir("generated/openapi-ops")

dependencyManagement {
    imports {
        mavenBom(libs.spring.modulith.bom.get().toString())
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.spring.boot.starter.actuator)
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Boot 4 keeps Flyway auto-configuration in this module; flyway-core alone is never auto-configured.
    implementation("org.springframework.boot:spring-boot-flyway")
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Boot 4 moved MockMvc's test slice out of spring-boot-starter-test into its own module.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(libs.archunit)
    testImplementation(libs.openapi.request.validator.spring.webmvc)
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgresql)
}

openApiGenerate {
    generatorName.set("spring")
    library.set("spring-boot")
    inputSpec.set(rootDir.resolve("../contracts/openapi.yaml").path)
    outputDir.set(openapiGenDir.get().asFile.path)
    apiPackage.set("com.tally.core.api")
    modelPackage.set("com.tally.core.api.model")
    cleanupOutput.set(true)
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "skipDefaultInterface" to "false",
            "openApiNullable" to "false",
            "useTags" to "true",
            "documentationProvider" to "none"
        )
    )
}

val openApiGenerateOps by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("spring")
    library.set("spring-boot")
    inputSpec.set(rootDir.resolve("../contracts/ops-openapi.yaml").path)
    outputDir.set(openapiOpsGenDir.get().asFile.path)
    apiPackage.set("com.tally.core.opsapi")
    modelPackage.set("com.tally.core.opsapi.model")
    cleanupOutput.set(true)
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "skipDefaultInterface" to "false",
            "openApiNullable" to "false",
            "useTags" to "true",
            "documentationProvider" to "none"
        )
    )
}

sourceSets {
    main {
        java {
            srcDir(openapiGenDir.map { it.dir("src/main/java") })
            srcDir(openapiOpsGenDir.map { it.dir("src/main/java") })
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
    dependsOn(openApiGenerateOps)
}

spotless {
    java {
        target("src/**/*.java")
        targetExclude("build/generated/**")
        palantirJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// contracts/ lives outside this Gradle project, so tests are told where it is instead of guessing
// from the working directory.
tasks.withType<Test>().configureEach {
    systemProperty("tally.contracts.openapi", rootDir.resolve("../contracts/openapi.yaml").path)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Tests tagged 'integration' (Testcontainers; Docker required)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    // An integration test that loses its @Tag("integration") would otherwise empty this suite silently.
    failOnNoDiscoveredTests = true
}

tasks.register<Test>("archTest") {
    group = "verification"
    description = "ArchUnit rules and Spring Modulith verify() (no Docker required)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter {
        includeTestsMatching("com.tally.core.architecture.*")
    }
    // Same silent-empty risk as integrationTest: renaming the package would void the suite.
    failOnNoDiscoveredTests = true
}

// Docker-free, so `./gradlew build` can enforce the architecture rules; integrationTest cannot.
tasks.check {
    dependsOn(tasks.named("archTest"))
}
