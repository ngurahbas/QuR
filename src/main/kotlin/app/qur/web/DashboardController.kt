package app.qur.web

import app.qur.security.JwtUserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

@Controller
class DashboardController {

    @GetMapping("/dashboard")
    fun dashboard(@AuthenticationPrincipal principal: JwtUserPrincipal, model: Model): Mono<String> {
        model.addAttribute("email", principal.email)
        return Mono.just("dashboard")
    }
}