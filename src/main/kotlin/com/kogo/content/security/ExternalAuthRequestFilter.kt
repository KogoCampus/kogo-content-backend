package com.kogo.content.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.logging.Logger
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException

@Component
class ExternalAuthRequestFilter(
    private val authenticationApiClient: AuthenticationApiClient
) : OncePerRequestFilter() {

    companion object : Logger()

    private val pathMatcher = AntPathMatcher()
    private val objectMapper = ObjectMapper()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            if (isAuthenticationRequiredUri(request.requestURI)) {
                val accessToken = getAccessTokenFromRequestHeader(request)
                val user = authenticationApiClient.authenticateOrCreateUser(accessToken)

                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                val authentication = UsernamePasswordAuthenticationToken(user.username, null, authorities)
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
