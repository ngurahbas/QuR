package app.qur.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

@Controller
class DashboardController {

    @GetMapping("/dashboard")
    fun dashboard(): Mono<String> {
        return Mono.just("dashboard")
    }
}