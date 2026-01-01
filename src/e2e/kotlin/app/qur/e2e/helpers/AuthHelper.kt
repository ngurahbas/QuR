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

		page.waitForURL("$baseUrl/**")
	}
}
