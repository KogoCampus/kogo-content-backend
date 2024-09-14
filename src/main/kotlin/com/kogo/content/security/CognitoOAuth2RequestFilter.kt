package com.kogo.content.security

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kogo.content.logging.Logger
import com.kogo.content.service.UserContextService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StringUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.DefaultUriBuilderFactory

@Component
class CognitoOAuth2RequestFilter(
    val userContextService: UserContextService,
) : OncePerRequestFilter() {

    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    lateinit var oauth2IssuerUri: String

    val uriFactory by lazy {
        val uriFactory = DefaultUriBuilderFactory(oauth2IssuerUri)
        uriFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
        uriFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
        uriFactory
    }

    val rs by lazy {
        val restTemplate = RestTemplate()
        restTemplate.uriTemplateHandler = uriFactory
        restTemplate
    }

    private val pathMatcher = AntPathMatcher()

    companion object : Logger() {
        const val COGNITO_GROUPS: String = "cognito:groups"
        const val COGNITO_USERNAME: String = "username"
        const val COGNITO_EMAIL: String = "email"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (isRequestURINotWhitelisted(request.requestURI)) {
            val accessToken = getAccessTokenFromRequestHeader(request)
            if (!accessToken.isNullOrBlank()) {
                try {
                    val userInfoJson = obtainUserInfo(accessToken)
                    val username = userInfoJson.get(COGNITO_USERNAME).toString()
                    val email = userInfoJson.get(COGNITO_EMAIL).toString()
                    if (username.isBlank() || email.isBlank())
                        throw HttpClientErrorException(HttpStatus.UNAUTHORIZED, "malformed userinfo received; username: $username, email: $email")
                    createNewUserProfileIfNotExist(username, email)
                } catch (e: HttpClientErrorException) {
                    log.error { e }
                }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun obtainUserInfo(accessToken: String): JsonNode {
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val res = rs.exchange("/oauth2/userInfo", HttpMethod.GET, HttpEntity<Any?>(headers), String::class.java)
        val objectMapper = ObjectMapper()
        return objectMapper.readTree(res.body)
    }

    private fun createNewUserProfileIfNotExist(username: String, email: String) {
        log.error { "test out $username, $email" }
        if (!userContextService.existsUserProfileByUsername(username)) {
            log.error { "test in $username, $email" }
            userContextService.createUserProfile(username, email)
        }
    }

    private fun getAccessTokenFromRequestHeader(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7)
        }
        return null
    }

    private fun isRequestURINotWhitelisted(requestURI: String): Boolean {
        return ! SecurityConfig.WHITELIST_PATHS.any { pathMatcher.match(it, requestURI) }
    }
}
