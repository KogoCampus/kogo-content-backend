package com.kogo.content.service.fileuploader

import com.kogo.content.exception.FileOperationFailure
import com.kogo.content.exception.FileOperationFailureException
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod

@Configuration
class FileUploaderConfiguration {
    @Bean
    @Profile("test")
    fun mockFileUploaderService(): FileUploaderService {
        return object : FileUploaderService() {
        }
    }

    @Bean
    @Profile("local")
    fun localFileUploaderService(): FileUploaderService {
        return FileUploaderService()
    }

    @Bean
    @Profile("stg || prd")
    fun prodFileUploaderService(): FileUploaderService {
        return object : FileUploaderService() {
            @PostConstruct
            fun checkConnection(){
                return try {
                    val response = restTemplate.exchange(
                        fileUploaderUrl,
                        HttpMethod.GET,
                        null,
                        object : ParameterizedTypeReference<Map<String, Any>>() {}
                    )
                    if (response.statusCode.is2xxSuccessful) {
                        val message = response.body!!["message"] as String
                        log.info { "File Uploader Server connected: $message" }
                    } else {
                        throw FileOperationFailureException(FileOperationFailure.CONNECT, null, "Failed to connect to File Uploader Server, status code: ${response.statusCode}")
                    }
                } catch (ex: Exception) {
                    throw FileOperationFailureException(FileOperationFailure.CONNECT, null, "Failed to connect to File Uploader Server, status code: ${ex.message}")
                }
            }
        }
    }
}
