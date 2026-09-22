plugins {
    java
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

// TLY-004 replaces these individually pinned deps with the Spring Boot BOM.
dependencies {
    implementation(libs.spring.web)
    implementation(libs.spring.context)
    implementation(libs.spring.core)
    implementation(libs.jakarta.servlet.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

sourceSets {
    main {
        java {
            srcDir(openapiGenDir.map { it.dir("src/main/java") })
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Placeholder until TLY-004 adds Testcontainers integration tests."
    dependsOn(tasks.test)
}

tasks.register("archTest") {
    group = "verification"
    description = "Placeholder until TLY-004 adds Spring Modulith verify() and ArchUnit rules."
}

tasks.register("spotlessCheck") {
    group = "verification"
    description = "Placeholder until TLY-004 adds the Spotless plugin."
}

tasks.register("spotlessApply") {
    group = "formatting"
    description = "Placeholder until TLY-004 adds the Spotless plugin."
}
