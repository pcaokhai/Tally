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

dependencyManagement {
    imports {
        mavenBom(libs.spring.modulith.bom.get().toString())
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.modulith:spring-modulith-starter-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
}

tasks.register("archTest") {
    group = "verification"
    description = "Placeholder until TLY-004 Task 2 adds Spring Modulith verify() and ArchUnit rules."
}
