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
		assertThat(page.locator("a:has-text('Continue with Keycloak')")).isVisible()
	}
}
