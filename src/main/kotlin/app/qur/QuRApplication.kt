package app.qur

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class QuRApplication

fun main(args: Array<String>) {
	runApplication<QuRApplication>(*args)
}
