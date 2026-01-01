package app.qur

import app.qur.test.IntegrationTestConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(IntegrationTestConfig::class)
class QuRApplicationTests {

	@Test
	fun contextLoads() {
	}
}
