# Plan: Move E2E Tests to Gradle Subproject

## Problem

JetBrains IDE does not properly recognize Playwright dependencies when using Gradle's `jvm-test-suite` plugin. The e2e tests in `src/e2e/` lack IDE support (autocomplete, navigation, error highlighting).

## Solution

Move e2e tests to a standalone Gradle subproject (`e2e/`) with its own `build.gradle.kts`.

## Goals

- Better IDE support for Playwright classes
- Clean separation from main project (no dependency on main project)
- Independent test execution (`./gradlew :e2e:test`)
- Root `./gradlew test` does NOT run e2e tests

---

## Target Structure

```
QuR/
├── settings.gradle.kts          # Add: include("e2e")
├── build.gradle.kts             # Remove: testing.suites block
├── e2e/
│   ├── build.gradle.kts         # New: standalone Kotlin/JUnit/Playwright
│   └── src/test/
│       ├── kotlin/app/qur/e2e/
│       │   ├── E2ETestBase.kt
│       │   ├── helpers/
│       │   │   └── AuthHelper.kt
│       │   ├── pages/
│       │   │   ├── DashboardPage.kt
│       │   │   ├── KeycloakLoginPage.kt
│       │   │   └── LoginPage.kt
│       │   └── scenarios/
│       │       └── AuthenticationFlowTest.kt
│       └── resources/
│           └── e2e.properties
└── src/main/...                  # Unchanged
```

---

## Tasks

### 1. Update `settings.gradle.kts`

Add subproject declaration:

```kotlin
rootProject.name = "QuR"

include("e2e")
```

### 2. Create `e2e/build.gradle.kts`

```kotlin
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
```

### 3. Move files

| From | To |
|------|-----|
| `src/e2e/kotlin/` | `e2e/src/test/kotlin/` |
| `src/e2e/resources/` | `e2e/src/test/resources/` |

Files to move:
- `E2ETestBase.kt`
- `helpers/AuthHelper.kt`
- `pages/DashboardPage.kt`
- `pages/KeycloakLoginPage.kt`
- `pages/LoginPage.kt`
- `scenarios/AuthenticationFlowTest.kt`
- `e2e.properties`

### 4. Update root `build.gradle.kts`

Remove the `testing.suites` block (lines 78-98):

```kotlin
// DELETE this entire block:
testing {
    suites {
        register<JvmTestSuite>("e2e") {
            // ...
        }
    }
}
```

Also remove `jvm-test-suite` plugin (line 7).

### 5. Delete old directory

Remove `src/e2e/` after confirming new structure works.

### 6. Update `specs/e2e_plan.md`

Update file paths and commands to reflect new structure:
- `./gradlew e2eTest` → `./gradlew :e2e:test`
- `src/e2e/` → `e2e/src/test/`

---

## Execution Order

1. Create `e2e/` directory structure
2. Create `e2e/build.gradle.kts`
3. Move test files to `e2e/src/test/`
4. Update `settings.gradle.kts`
5. Remove `testing.suites` from root `build.gradle.kts`
6. Verify IDE recognizes Playwright (`./gradlew :e2e:dependencies`)
7. Run tests (`./gradlew :e2e:test`)
8. Delete `src/e2e/`
9. Update documentation

---

## Verification

```bash
# Check subproject is recognized
./gradlew projects

# Check dependencies are resolved
./gradlew :e2e:dependencies --configuration testRuntimeClasspath

# Run e2e tests
./gradlew :e2e:test

# Verify root test doesn't run e2e
./gradlew test  # Should NOT run e2e tests
```

---

## Rollback

If issues occur, revert by:
1. Restore `testing.suites` block in root `build.gradle.kts`
2. Move files back from `e2e/src/test/` to `src/e2e/`
3. Remove `include("e2e")` from `settings.gradle.kts`
4. Delete `e2e/` directory
