plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
