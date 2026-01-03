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
	@DisplayName("Login page displays Keycloak provider and completes OAuth2 login")
	fun `login page should display Keycloak provider and complete OAuth2 login`() {
		val loginPage = LoginPage(page, baseUrl)
		loginPage.navigate()

		loginPage.assertPageLoaded()
		loginPage.assertKeycloakProviderVisible()

		val keycloakPage = loginPage.clickKeycloakLogin()
		keycloakPage.waitForLoad()
		keycloakPage.assertLoginFormVisible()

		keycloakPage.login(testUsername, testPassword)

		val dashboardPage = DashboardPage(page, baseUrl)
		dashboardPage.waitForLoad()
		assertTrue(dashboardPage.isLoaded(), "Dashboard should be loaded after login")

		dashboardPage.assertUserEmailDisplayed(testEmail)
	}

	@Test
	@DisplayName("Dashboard profile menu opens and shows options")
	fun `dashboard should open profile menu with Alpine js`() {
		AuthHelper.loginAsUser(page, baseUrl, testUsername, testPassword)

		val dashboardPage = DashboardPage(page, baseUrl)
		dashboardPage.waitForLoad()

		dashboardPage.openProfileMenu()

		dashboardPage.assertProfileMenuItems()
	}

	@Test
	@DisplayName("Unauthenticated access to dashboard redirects to login")
	fun `should redirect to login when accessing dashboard without auth`() {
		page.navigate("$baseUrl/dashboard")

		page.waitForLoadState()

		val currentUrl = page.url()
		assertTrue(
			!currentUrl.contains("/dashboard") || currentUrl.contains("login") || currentUrl.contains("oauth"),
			"Should redirect away from dashboard when not authenticated"
		)
	}
}
