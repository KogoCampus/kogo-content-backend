package com.kogo.content.security

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.logging.Logger
import com.kogo.content.service.UserService
import com.kogo.content.storage.entity.UserIdToken
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StringUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.client.HttpServerErrorException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException

@Component
class ExternalAuthRequestFilter (
    val userService: UserService
) : OncePerRequestFilter() {

    @Value("\${kogo-api.base-url}")
    lateinit var kogoApiUrl: String

    @Value("\${compile-version-key}")
    lateinit var compileVersionKey: String

    private val restTemplate = RestTemplate()
    private val pathMatcher = AntPathMatcher()
    private val objectMapper = ObjectMapper()

    companion object : Logger() {
        const val AUTH_ENDPOINT = "/student/authenticate"
        const val USERDATA = "userdata"
        const val USERNAME = "username"
        const val EMAIL = "email"
        const val SCHOOLINFO = "schoolData"
        const val SCHOOL_NAME = "name"
        const val SCHOOL_SHORTENED_NAME = "shortenedName"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            if (isAuthenticationRequiredUri(request.requestURI)) {
                val accessToken = getAccessTokenFromRequestHeader(request)
                // Call external authentication API
                val userInfoJson = authenticateUserWithApi(accessToken).get(USERDATA)
                val username = userInfoJson.get(USERNAME).toString().removeSurrounding("\"")

                if (userService.findUserProfileByUsername(username) == null) {
                    val email = userInfoJson.get(EMAIL).toString().removeSurrounding("\"")
                    val schoolInfoJson = userInfoJson.get(SCHOOLINFO)
                    val schoolName = schoolInfoJson.get(SCHOOL_NAME).toString().removeSurrounding("\"")
                    val schoolShortenedName = schoolInfoJson.get(SCHOOL_SHORTENED_NAME).toString().removeSurrounding("\"")
                    userService.createUserProfile(
                        idToken = UserIdToken(username, email),
                        username = username,
                        email = email,
                        schoolName = schoolName,
                        schoolShortenedName = schoolShortenedName,)
                }
                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER")) // You can assign roles or authorities
                val authentication = UsernamePasswordAuthenticationToken(username, null, authorities)
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (ex: Exception) {
            when (ex) {
                is AuthenticationException -> handleAuthenticationException(response, ex)
                else -> handleUnexpectedException(response, ex)
            }
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticateUserWithApi(accessToken: String): JsonNode {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
            set("APP-VERSION-KEY", compileVersionKey)
        }
        val entity = HttpEntity<Any?>(headers)
        return try {
            val response = restTemplate.exchange(
                "$kogoApiUrl$AUTH_ENDPOINT?grant_type=access_token",
                HttpMethod.GET, entity, String::class.java
            )
            if (response.statusCode != HttpStatus.OK) {
                throw BadCredentialsException("External authentication failed with status code: ${response.statusCode.value()}")
            }
            val objectMapper = ObjectMapper()
            objectMapper.readTree(response.body)
        } catch (ex: HttpServerErrorException) {
            throw RuntimeException("Failed to connect to the external authentication server - ${ex.statusCode} - ${ex.responseBodyAsString}")
        }
    }

    private fun getAccessTokenFromRequestHeader(request: HttpServletRequest): String {
        val bearerToken = request.getHeader("Authorization")
        return bearerToken.takeIf { StringUtils.hasText(it) && bearerToken.startsWith("Bearer") }
            ?.substring(7)
            ?: throw BadCredentialsException("Cannot read access token.")
    }

    private fun isAuthenticationRequiredUri(requestURI: String): Boolean {
        return !SecurityConfig.WHITELIST_PATHS.any { pathMatcher.match(it, requestURI) }
    }

    private fun handleAuthenticationException(response: HttpServletResponse, ex: AuthenticationException) {
        log.error { "Authentication Failed: ${ex.message}" }
        val errorResponse = HttpJsonResponse.ErrorResponse(
            error = ErrorCode.UNAUTHORIZED.name,
            details = ex.message ?: "Your access token is invalid or missing."
        )
        writeErrorResponse(response, errorResponse, ErrorCode.UNAUTHORIZED.httpStatus)
    }

    private fun handleUnexpectedException(response: HttpServletResponse, ex: Exception) {
        log.error(ex) { "Unhandled filter exception: ${ex.message}" }
        val errorResponse = HttpJsonResponse.ErrorResponse(
            error = ErrorCode.INTERNAL_SERVER_ERROR.name,
            details = "An unexpected error occurred"
        )
        writeErrorResponse(response, errorResponse, ErrorCode.INTERNAL_SERVER_ERROR.httpStatus)
    }

    private fun writeErrorResponse(response: HttpServletResponse, errorResponse: HttpJsonResponse.ErrorResponse, status: HttpStatus) {
        response.status = status.value()
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
