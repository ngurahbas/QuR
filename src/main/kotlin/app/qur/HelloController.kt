package app.qur

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.result.view.Rendering
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Controller
class HelloController {

    @GetMapping("/")
    fun home(): Mono<ResponseEntity<Void>> {
        return Mono.just(ResponseEntity.status(302).location(java.net.URI.create("/hello")).build())
    }

    @GetMapping("/hello")
    fun hello(): Mono<ResponseEntity<String>> {
        return Mono.just(ResponseEntity.ok("Hello, World!"))
    }
}