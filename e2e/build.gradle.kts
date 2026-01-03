plugins {
    kotlin("jvm") version "2.3.0"
}

group = "app.qur"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("com.microsoft.playwright:playwright:1.49.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()

    environment("HEADLESS", System.getenv("HEADLESS") ?: "true")
    environment("BASE_URL", System.getenv("BASE_URL") ?: "http://localhost:8080")
    environment("KEYCLOAK_URL", System.getenv("KEYCLOAK_URL") ?: "http://localhost:8081")
}
