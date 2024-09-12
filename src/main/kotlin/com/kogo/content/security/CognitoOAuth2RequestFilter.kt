package com.kogo.content.security

import com.kogo.content.endpoint.controller.TopicController.Companion.log
import com.kogo.content.logging.Logger
import com.kogo.content.service.AuthenticatedUserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.util.StringUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.DefaultUriBuilderFactory


class CognitoOAuth2RequestFilter : OncePerRequestFilter() {

    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private lateinit var oauth2IssuerUri: String

    @Autowired
    private lateinit var authenticatedUserService: AuthenticatedUserService

    private val rs = RestTemplate()

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
        val accessToken = getAccessTokenFromRequestHeader(request)
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        try {
            val res = rs.exchange("/oauth2/userInfo", HttpMethod.GET, HttpEntity<Any?>(headers), String::class.java).body ?: throw RuntimeException("authorization failure")
            val json = JSONObject(res)
            val username = json.optString(COGNITO_USERNAME, null)
            val email = json.optString(COGNITO_EMAIL, null)
            if (username == null || email == null)
                throw RuntimeException("authorization failure, invalid userinfo username: $username, email: $email")
            authenticatedUserService.createUserProfile(username, email)
        } catch (e: RuntimeException) {
            log.error { e }
            return
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
}
