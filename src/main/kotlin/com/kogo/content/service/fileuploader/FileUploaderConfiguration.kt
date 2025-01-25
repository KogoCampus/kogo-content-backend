package com.kogo.content.service.fileuploader

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class FileUploaderConfiguration {
    @Bean
    @Profile("test")
    fun mockFileUploaderService(): FileUploaderService {
        return object : FileUploaderService() {
        }
    }

    @Bean
    @Profile("!test")
    fun prodFileUploaderService(): FileUploaderService {
        return FileUploaderService()
    }
}
