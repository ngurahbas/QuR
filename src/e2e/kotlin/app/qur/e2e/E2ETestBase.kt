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

		val recordVideo = System.getenv("RECORD_VIDEO")?.toBoolean() ?: false
		val headless = System.getenv("HEADLESS")?.toBoolean()
			?: config.getProperty("playwright.headless", "true").toBoolean()
		val slowMo = if (recordVideo) {
			System.getenv("VIDEO_SLOW_MO")?.toDoubleOrNull()
				?: config.getProperty("playwright.video.slow-mo", "100").toDouble()
		} else {
			config.getProperty("playwright.slow-mo", "0").toDouble()
		}

		playwright = Playwright.create()
		browser = playwright.chromium().launch(
			BrowserType.LaunchOptions()
				.setHeadless(headless)
				.setSlowMo(slowMo)
		)
	}

	@BeforeEach
	fun setupContext() {
		val recordVideo = System.getenv("RECORD_VIDEO")?.toBoolean() ?: false
		val videoDir = System.getenv("VIDEO_DIR") ?: config.getProperty("playwright.video-dir", "build/videos")

		val contextOptions = Browser.NewContextOptions()
			.setViewportSize(1600, 900)

		if (recordVideo) {
			java.io.File(videoDir).mkdirs()
			contextOptions.setRecordVideoDir(java.nio.file.Paths.get(videoDir))
		}

		context = browser.newContext(contextOptions)
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
