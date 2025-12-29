package app.qur

import app.qur.db.TestContainersConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Import(TestContainersConfig::class)
@Testcontainers
class QuRApplicationTests {

	@Test
	fun contextLoads() {
	}
}
