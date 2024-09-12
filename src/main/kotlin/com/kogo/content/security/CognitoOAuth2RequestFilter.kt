package com.kogo.content.security

import com.kogo.content.logging.Logger
import com.kogo.content.service.AuthenticatedUserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.util.AntPathMatcher
import org.springframework.util.StringUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.DefaultUriBuilderFactory


class CognitoOAuth2RequestFilter(
    private val oauth2IssuerUri: String,
    private val whitelistPaths: Array<String>,
) : OncePerRequestFilter() {

    @Autowired
    private lateinit var authenticatedUserService: AuthenticatedUserService

    private val rs = RestTemplate()

    private val pathMatcher = AntPathMatcher()

    init {
        val uriFactory = DefaultUriBuilderFactory(oauth2IssuerUri)
        uriFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
        uriFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
        rs.uriTemplateHandler = uriFactory
    }

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
            val headers = HttpHeaders()
            headers.set("Authorization", "Bearer $accessToken")
            try {
                val res = rs.exchange("/oauth2/userInfo", HttpMethod.GET, HttpEntity<Any?>(headers), String::class.java).body
                val json = JSONObject(res)
                val username = json.optString(COGNITO_USERNAME, null)
                val email = json.optString(COGNITO_EMAIL, null)
                if (username == null || email == null)
                    throw HttpClientErrorException(HttpStatus.UNAUTHORIZED, "malformed userinfo received; username: $username, email: $email")
                authenticatedUserService.createUserProfile(username, email)
            } catch (e: HttpClientErrorException) {
                log.error { e }
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun getAccessTokenFromRequestHeader(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7)
        }
        return null
    }

    private fun isRequestURINotWhitelisted(requestURI: String): Boolean {
        return ! whitelistPaths.any { pathMatcher.match(it, requestURI) }
    }
}
