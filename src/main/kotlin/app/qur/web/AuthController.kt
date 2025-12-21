package app.qur.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

@Controller
class AuthController {

    @GetMapping("/login")
    fun login(): Mono<String> {
        return Mono.just("login")
    }
}