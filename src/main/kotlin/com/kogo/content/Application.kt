package com.kogo.content

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val logger = KotlinLogging.logger() {}
    runApplication<Application>(*args) {
        setBannerMode(Banner.Mode.CONSOLE);
        logger.debug { "test" }
        logger.info { "test info" }
    }
}
