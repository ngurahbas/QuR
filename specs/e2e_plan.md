# E2E Testing Plan for QuR Application

## Overview

This plan establishes end-to-end testing for the QuR application using Playwright with Kotlin. Tests will run against real docker-compose services (PostgreSQL, Keycloak, Valkey) to verify the complete authentication flow.

### Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Directory | `src/e2e/` | Separate from unit/integration tests |
| Infrastructure | Real docker-compose services | No mocks, tests actual OAuth2 flow |
| App Startup | Manual | Easier debugging, start app before tests |
| Test Data | Keep state after tests | Faster iteration, can inspect data |
| Browser | Chromium via Playwright | Headless by default |
| Test User | user1 / password1 | Already configured in Keycloak setup script |

---

## Prerequisites

Before running E2E tests:

```bash
# 1. Start docker-compose services
docker-compose up -d

# 2. Wait for Keycloak initialization (watch for "setup complete")
docker-compose logs -f keycloak-init

# 3. Start Spring Boot app (in another terminal)
./gradlew bootRun

# 4. Install Playwright browsers (first time only)
npm run install:playwright
```

---

## File Structure

```
QuR/
├── src/e2e/
│   ├── kotlin/app/qur/e2e/
│   │   ├── E2ETestBase.kt              # Base class with Playwright setup
│   │   ├── pages/
│   │   │   ├── LoginPage.kt            # /logins page object
│   │   │   ├── KeycloakLoginPage.kt    # Keycloak login form
│   │   │   └── DashboardPage.kt        # /dashboard page object
│   │   ├── scenarios/
│   │   │   └── AuthenticationFlowTest.kt  # Happy path test
│   │   └── helpers/
│   │       └── AuthHelper.kt           # Reusable login helper
│   └── resources/
│       └── e2e.properties              # Test configuration
├── specs/
│   └── e2e_plan.md                     # This document
└── build.gradle.kts                    # Updated with e2e source set
```

---

## Implementation Details

### 1. Gradle Configuration (`build.gradle.kts`)

Add e2e source set and Playwright dependency:

```kotlin
// Add after existing sourceSets or create new block
sourceSets {
    create("e2e") {
        kotlin.srcDir("src/e2e/kotlin")
        resources.srcDir("src/e2e/resources")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

// Add e2e configuration
val e2eImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val e2eRuntimeOnly by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get())
}

dependencies {
    // ... existing dependencies ...
    
    // E2E testing with Playwright
    e2eImplementation("com.microsoft.playwright:playwright:1.49.0")
    e2eImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

// E2E test task
tasks.register<Test>("e2eTest") {
    group = "verification"
    description = "Run E2E tests with Playwright"
    testClassesDirs = sourceSets["e2e"].output.classesDirs
    classpath = sourceSets["e2e"].runtimeClasspath
    useJUnitPlatform()
    
    // Pass environment variables
    environment("HEADLESS", System.getenv("HEADLESS") ?: "true")
    environment("BASE_URL", System.getenv("BASE_URL") ?: "http://localhost:8080")
    environment("KEYCLOAK_URL", System.getenv("KEYCLOAK_URL") ?: "http://localhost:8081")
}
```

### 2. E2E Configuration (`src/e2e/resources/e2e.properties`)

```properties
# Application URLs
app.base-url=http://localhost:8080
keycloak.url=http://localhost:8081

# Test user credentials (configured in keycloak/setup-keycloak.sh)
test.user.username=user1
test.user.password=password1
test.user.email=user1@example.com

# Playwright settings
playwright.headless=true
playwright.slow-mo=0
playwright.timeout=30000
```

### 3. Base Test Class (`src/e2e/kotlin/app/qur/e2e/E2ETestBase.kt`)

```kotlin
package app.qur.e2e

import com.microsoft.playwright.*
import org.junit.jupiter.api.*
import java.util.Properties

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class E2ETestBase {

    companion object {
        lateinit var playwright: Playwright
        lateinit var browser: Browser
        lateinit var config: Properties
    }

    protected lateinit var context: BrowserContext
    protected lateinit var page: Page

    protected val baseUrl: String get() = config.getProperty("app.base-url", "http://localhost:8080")
    protected val keycloakUrl: String get() = config.getProperty("keycloak.url", "http://localhost:8081")
    protected val testUsername: String get() = config.getProperty("test.user.username", "user1")
    protected val testPassword: String get() = config.getProperty("test.user.password", "password1")
    protected val testEmail: String get() = config.getProperty("test.user.email", "user1@example.com")

    @BeforeAll
    fun setupPlaywright() {
        config = Properties().apply {
            E2ETestBase::class.java.getResourceAsStream("/e2e.properties")?.use { load(it) }
        }
        
        val headless = System.getenv("HEADLESS")?.toBoolean() 
            ?: config.getProperty("playwright.headless", "true").toBoolean()
        val slowMo = config.getProperty("playwright.slow-mo", "0").toDouble()

        playwright = Playwright.create()
        browser = playwright.chromium().launch(
            BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo)
        )
    }

    @BeforeEach
    fun setupContext() {
        context = browser.newContext()
        page = context.newPage()
        page.setDefaultTimeout(
            config.getProperty("playwright.timeout", "30000").toDouble()
        )
    }

    @AfterEach
    fun teardownContext() {
        context.close()
    }

    @AfterAll
    fun teardownPlaywright() {
        browser.close()
        playwright.close()
    }
}
```

### 4. Page Objects

#### LoginPage (`src/e2e/kotlin/app/qur/e2e/pages/LoginPage.kt`)

```kotlin
package app.qur.e2e.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat

class LoginPage(private val page: Page, private val baseUrl: String) {

    fun navigate(): LoginPage {
        page.navigate("$baseUrl/logins")
        return this
    }

    fun isLoaded(): Boolean {
        return page.title().contains("Login") &&
               page.locator("h1:has-text('Sign in to your account')").isVisible
    }

    fun hasKeycloakProvider(): Boolean {
        return page.locator("a:has-text('Continue with Keycloak')").isVisible
    }

    fun clickKeycloakLogin(): KeycloakLoginPage {
        page.locator("a:has-text('Continue with Keycloak')").click()
        return KeycloakLoginPage(page)
    }

    fun assertPageLoaded() {
        assertThat(page.locator("h1")).containsText("Sign in to your account")
    }

    fun assertKeycloakProviderVisible() {
        assertThat(page.locator("a:has-text('Continue with Keycloak')")).isVisible
    }
}
```

#### KeycloakLoginPage (`src/e2e/kotlin/app/qur/e2e/pages/KeycloakLoginPage.kt`)

```kotlin
package app.qur.e2e.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat

class KeycloakLoginPage(private val page: Page) {

    fun isLoaded(): Boolean {
        return page.url().contains("localhost:8081") &&
               page.locator("#username").isVisible
    }

    fun waitForLoad() {
        page.waitForURL("**/realms/qur/protocol/openid-connect/auth**")
        page.locator("#username").waitFor()
    }

    fun login(username: String, password: String) {
        page.locator("#username").fill(username)
        page.locator("#password").fill(password)
        page.locator("#kc-login").click()
    }

    fun assertLoginFormVisible() {
        assertThat(page.locator("#username")).isVisible
        assertThat(page.locator("#password")).isVisible
        assertThat(page.locator("#kc-login")).isVisible
    }
}
```

#### DashboardPage (`src/e2e/kotlin/app/qur/e2e/pages/DashboardPage.kt`)

```kotlin
package app.qur.e2e.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat

class DashboardPage(private val page: Page, private val baseUrl: String) {

    fun navigate(): DashboardPage {
        page.navigate("$baseUrl/dashboard")
        return this
    }

    fun isLoaded(): Boolean {
        return page.url().contains("/dashboard")
    }

    fun waitForLoad() {
        page.waitForURL("$baseUrl/dashboard")
    }

    fun getUserEmail(): String {
        return page.locator("button:has(svg) span.text-sm").textContent() ?: ""
    }

    fun openProfileMenu() {
        page.locator("button:has(svg):has-text('@')").click()
    }

    fun isProfileMenuOpen(): Boolean {
        return page.locator("text=Profile").isVisible &&
               page.locator("text=Settings").isVisible &&
               page.locator("text=Logout").isVisible
    }

    fun assertUserEmailDisplayed(expectedEmail: String) {
        assertThat(page.locator("button span.text-sm")).containsText(expectedEmail)
    }

    fun assertProfileMenuItems() {
        assertThat(page.locator("button:has-text('Profile')")).isVisible
        assertThat(page.locator("button:has-text('Settings')")).isVisible
        assertThat(page.locator("button:has-text('Logout')")).isVisible
    }
}
```

### 5. Auth Helper (`src/e2e/kotlin/app/qur/e2e/helpers/AuthHelper.kt`)

```kotlin
package app.qur.e2e.helpers

import app.qur.e2e.pages.KeycloakLoginPage
import app.qur.e2e.pages.LoginPage
import com.microsoft.playwright.Page

object AuthHelper {

    fun loginAsUser(
        page: Page,
        baseUrl: String,
        username: String,
        password: String
    ) {
        val loginPage = LoginPage(page, baseUrl)
        loginPage.navigate()
        
        val keycloakPage = loginPage.clickKeycloakLogin()
        keycloakPage.waitForLoad()
        keycloakPage.login(username, password)
        
        // Wait for redirect back to app
        page.waitForURL("$baseUrl/**")
    }
}
```

### 6. Test Scenario (`src/e2e/kotlin/app/qur/e2e/scenarios/AuthenticationFlowTest.kt`)

```kotlin
package app.qur.e2e.scenarios

import app.qur.e2e.E2ETestBase
import app.qur.e2e.helpers.AuthHelper
import app.qur.e2e.pages.DashboardPage
import app.qur.e2e.pages.KeycloakLoginPage
import app.qur.e2e.pages.LoginPage
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@DisplayName("Authentication Flow E2E Tests")
class AuthenticationFlowTest : E2ETestBase() {

    @Test
    @DisplayName("Login page displays Keycloak provider")
    fun `login page should display Keycloak provider`() {
        val loginPage = LoginPage(page, baseUrl)
        loginPage.navigate()

        loginPage.assertPageLoaded()
        loginPage.assertKeycloakProviderVisible()
    }

    @Test
    @DisplayName("Complete OAuth2 login flow reaches dashboard")
    fun `should complete OAuth2 login and reach dashboard with user email`() {
        // Navigate to login page
        val loginPage = LoginPage(page, baseUrl)
        loginPage.navigate()
        assertTrue(loginPage.isLoaded(), "Login page should be loaded")

        // Click Keycloak login
        val keycloakPage = loginPage.clickKeycloakLogin()
        keycloakPage.waitForLoad()
        keycloakPage.assertLoginFormVisible()

        // Login with test credentials
        keycloakPage.login(testUsername, testPassword)

        // Verify redirect to dashboard
        val dashboardPage = DashboardPage(page, baseUrl)
        dashboardPage.waitForLoad()
        assertTrue(dashboardPage.isLoaded(), "Dashboard should be loaded after login")

        // Verify user email is displayed
        dashboardPage.assertUserEmailDisplayed(testEmail)
    }

    @Test
    @DisplayName("Dashboard profile menu opens and shows options")
    fun `dashboard should open profile menu with Alpine js`() {
        // Login first
        AuthHelper.loginAsUser(page, baseUrl, testUsername, testPassword)

        // Navigate to dashboard
        val dashboardPage = DashboardPage(page, baseUrl)
        dashboardPage.waitForLoad()

        // Open profile menu
        dashboardPage.openProfileMenu()

        // Verify menu items (Alpine.js dropdown)
        dashboardPage.assertProfileMenuItems()
    }

    @Test
    @DisplayName("Unauthenticated access to dashboard redirects to login")
    fun `should redirect to login when accessing dashboard without auth`() {
        // Try to access dashboard directly without login
        page.navigate("$baseUrl/dashboard")

        // Should be redirected to login or OAuth flow
        page.waitForLoadState()

        // Verify not on dashboard
        val currentUrl = page.url()
        assertTrue(
            !currentUrl.contains("/dashboard") || currentUrl.contains("login") || currentUrl.contains("oauth"),
            "Should redirect away from dashboard when not authenticated"
        )
    }
}
```

---

## Running E2E Tests

### Quick Start

```bash
# 1. Start docker-compose services
docker-compose up -d

# 2. Wait for Keycloak to be ready (watch for "setup complete")
docker-compose logs -f keycloak-init

# 3. In another terminal, start the app
./gradlew bootRun

# 4. In another terminal, run E2E tests
./gradlew e2eTest
```

### Running with Headed Browser (Debug Mode)

```bash
HEADLESS=false ./gradlew e2eTest
```

### Running Specific Test

```bash
./gradlew e2eTest --tests "AuthenticationFlowTest"
```

---

## Package.json Update

Add Playwright installation script:

```json
{
  "scripts": {
    "install:playwright": "npx playwright install chromium"
  },
  "devDependencies": {
    "playwright": "^1.49.0"
  }
}
```

---

## Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `specs/e2e_plan.md` | Create | This plan document |
| `build.gradle.kts` | Modify | Add e2e source set and dependencies |
| `package.json` | Modify | Add playwright dev dependency and script |
| `src/e2e/resources/e2e.properties` | Create | Test configuration |
| `src/e2e/kotlin/app/qur/e2e/E2ETestBase.kt` | Create | Base test class |
| `src/e2e/kotlin/app/qur/e2e/pages/LoginPage.kt` | Create | Login page object |
| `src/e2e/kotlin/app/qur/e2e/pages/KeycloakLoginPage.kt` | Create | Keycloak page object |
| `src/e2e/kotlin/app/qur/e2e/pages/DashboardPage.kt` | Create | Dashboard page object |
| `src/e2e/kotlin/app/qur/e2e/helpers/AuthHelper.kt` | Create | Auth helper |
| `src/e2e/kotlin/app/qur/e2e/scenarios/AuthenticationFlowTest.kt` | Create | Test scenarios |

**Total: 10 files (2 modify, 8 create)**

---

## Troubleshooting

### Keycloak not ready

```bash
# Check if Keycloak is running
curl http://localhost:8081/realms/qur/.well-known/openid-configuration

# Check if user exists
docker-compose logs keycloak-init
```

### App not connecting to services

```bash
# Verify services are running
docker-compose ps

# Check app logs for connection errors
./gradlew bootRun --info
```

### Playwright browser issues

```bash
# Reinstall browsers
npx playwright install chromium --force

# Run with debug logging
DEBUG=pw:browser ./gradlew e2eTest
```

### Test user login fails

```bash
# Re-run Keycloak init to ensure user exists
docker-compose restart keycloak-init
docker-compose logs -f keycloak-init
```
