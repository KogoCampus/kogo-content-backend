package com.kogo.content.endpoint

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfiguration : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(RequestInterceptor()).addPathPatterns("/media/**")
        super.addInterceptors(registry)
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("*") // Allow all origins, or specify allowed origins
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow specific HTTP methods
            .allowedHeaders("Authorization", "Content-Type", "Accept") // Allow specific headers
            .exposedHeaders("Authorization", "Content-Disposition") // Expose specific headers to client
            .allowCredentials(true) // Allow credentials (cookies, authorization headers)
            .maxAge(3600) // Cache pre-flight request for 1 hour
    }
}
