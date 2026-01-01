package app.qur.e2e.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat

class KeycloakLoginPage(private val page: Page) {

	fun isLoaded(): Boolean {
		return page.url().contains("localhost:8081") &&
			page.locator("#username").isVisible
	}

	fun waitForLoad() {
		page.locator("#username").waitFor()
	}

	fun login(username: String, password: String) {
		page.locator("#username").fill(username)
		page.locator("#password").fill(password)
		page.locator("#kc-login").click()
	}

	fun assertLoginFormVisible() {
		assertThat(page.locator("#username")).isVisible()
		assertThat(page.locator("#password")).isVisible()
		assertThat(page.locator("#kc-login")).isVisible()
	}
}
