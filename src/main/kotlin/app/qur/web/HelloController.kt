package app.qur.web

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import reactor.core.publisher.Mono
import java.net.URI

@Controller
class HelloController {

    @GetMapping("/")
    fun home(): Mono<ResponseEntity<Void>> {
        return Mono.just(ResponseEntity.status(302).location(URI.create("/hello")).build())
    }

    @GetMapping("/hello")
    fun hello(): Mono<ResponseEntity<String>> {
        return Mono.just(ResponseEntity.ok("Hello, World!"))
    }
}