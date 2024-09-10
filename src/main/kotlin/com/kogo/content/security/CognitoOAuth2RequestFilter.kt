package com.kogo.content.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.util.StringUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.DefaultUriBuilderFactory


class CognitoOAuth2RequestFilter(
    private val issuerUri: String,
) : OncePerRequestFilter() {

    companion object {
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
        val uriFactory = DefaultUriBuilderFactory(issuerUri)
        uriFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
        val rs = RestTemplate()
        val headers = HttpHeaders()
        rs.uriTemplateHandler = uriFactory
        headers.set("Authorization", "Bearer $accessToken")
        try {
            val res = rs.exchange("/oauth2/userInfo", HttpMethod.GET, HttpEntity<Any?>(headers), String::class.java)
            println("testtest")
            println(res)
            println(res.statusCode)
        } catch (e: RuntimeException) {
            println(e.message)
            println("errortest")
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
