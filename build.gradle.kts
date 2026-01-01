plugins {
	kotlin("jvm") version "2.3.0"
	kotlin("plugin.spring") version "2.3.0"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.graalvm.buildtools.native") version "0.11.1"
}

group = "app"
version = "0.0.1-SNAPSHOT"
description = "QuR"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-mustache")
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
	developmentOnly("org.springframework.boot:spring-boot-starter-actuator")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:postgresql:1.21.4")
	testImplementation("org.testcontainers:r2dbc:1.21.4")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.projectreactor:reactor-test")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.register<Exec>("compileTailwind") {
	group = "build"
	description = "Compile and minify Tailwind CSS"
	workingDir = projectDir
	commandLine = listOf(
		"node_modules/.bin/tailwindcss",
		"--input", "tailwind.src.css", 
		"--output", "src/main/resources/static/css/tailwind.css",
		"--minify"
	)
	
	doFirst {
		mkdir("src/main/resources/static/css")
	}
}

tasks.named("processResources") {
	dependsOn("compileTailwind")
}

sourceSets {
	create("e2e") {
		kotlin.srcDir("src/e2e/kotlin")
		resources.srcDir("src/e2e/resources")
		compileClasspath += sourceSets.main.get().output
		runtimeClasspath += sourceSets.main.get().output
	}
}

val e2eImplementation by configurations.getting {
	extendsFrom(configurations.implementation.get())
}

val e2eRuntimeOnly by configurations.getting {
	extendsFrom(configurations.runtimeOnly.get())
}

dependencies {
	e2eImplementation("com.microsoft.playwright:playwright:1.49.0")
	e2eImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
	e2eImplementation("org.jetbrains.kotlin:kotlin-test")
	e2eRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register<Test>("e2eTest") {
	group = "verification"
	description = "Run E2E tests with Playwright"
	testClassesDirs = sourceSets["e2e"].output.classesDirs
	classpath = sourceSets["e2e"].runtimeClasspath
	useJUnitPlatform()

	environment("HEADLESS", System.getenv("HEADLESS") ?: "true")
	environment("BASE_URL", System.getenv("BASE_URL") ?: "http://localhost:8080")
	environment("KEYCLOAK_URL", System.getenv("KEYCLOAK_URL") ?: "http://localhost:8081")
}

tasks.named<Copy>("processE2eResources") {
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Test> {
	useJUnitPlatform()
}