package com.kogo.content.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration


@Configuration
class SecurityConfig {

    companion object {
        val WHITELIST_PATHS = arrayOf(
            "/-/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
        )
    }

    @Autowired
    lateinit var externalAuthRequestFilter: ExternalAuthRequestFilter

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .cors { it.configurationSource { CorsConfiguration().applyPermitDefaultValues() } }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // disable server-side session user
        .headers { it.frameOptions { frameOptions -> frameOptions.sameOrigin() } }
        .authorizeHttpRequests {
            it.requestMatchers(*WHITELIST_PATHS).permitAll()
                .anyRequest().authenticated()
        }
        .addFilterBefore(externalAuthRequestFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()

    @Bean
    fun authenticationManager(
        authenticationConfiguration: AuthenticationConfiguration
    ): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }
}
