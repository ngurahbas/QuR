package app.qur

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono

@Controller
class HelloController {

    @GetMapping("/hello")
    fun hello(): Mono<ResponseEntity<String>> {
        return Mono.just(ResponseEntity.ok("Hello, World!"))
    }
}