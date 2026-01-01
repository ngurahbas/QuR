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
		assertThat(page.locator("button:has-text('Profile')")).isVisible()
		assertThat(page.locator("button:has-text('Settings')")).isVisible()
		assertThat(page.locator("button:has-text('Logout')")).isVisible()
	}
}
