package app.qur.web

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

@Controller
class DashboardController {

    @GetMapping("/dashboard")
    fun dashboard(@AuthenticationPrincipal oidcUser: OidcUser, model: Model): Mono<String> {
        model.addAttribute("email", oidcUser.email)
        return Mono.just("dashboard")
    }
}