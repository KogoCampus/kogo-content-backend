//package com.kogo.content.security
//
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.kogo.content.logging.Logger
//import com.kogo.content.service.entity.UserContextService
//import jakarta.servlet.FilterChain
//import jakarta.servlet.http.HttpServletRequest
//import jakarta.servlet.http.HttpServletResponse
//import org.apache.tomcat.websocket.AuthenticationException
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.http.HttpEntity
//import org.springframework.http.HttpHeaders
//import org.springframework.http.HttpMethod
//import org.springframework.security.oauth2.jwt.Jwt
//import org.springframework.security.oauth2.jwt.JwtDecoder
//import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
//import org.springframework.stereotype.Component
//import org.springframework.util.AntPathMatcher
//import org.springframework.util.StringUtils
//import org.springframework.web.client.RestTemplate
//import org.springframework.web.filter.OncePerRequestFilter
//import org.springframework.web.util.DefaultUriBuilderFactory
//
//@Component
//class CognitoOAuth2RequestFilter(
//    val userContextService: UserContextService
//) : OncePerRequestFilter() {
//
//    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
//    lateinit var oauth2IssuerUri: String
//
//    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
//    lateinit var jwkSetUri: String
//
//    val uriFactory by lazy {
//        val uriFactory = DefaultUriBuilderFactory(oauth2IssuerUri)
//        uriFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
//        uriFactory.encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
//        uriFactory
//    }
//
//    val rs by lazy {
//        val restTemplate = RestTemplate()
//        restTemplate.uriTemplateHandler = uriFactory
//        restTemplate
//    }
//
//    private val pathMatcher = AntPathMatcher()
//
//    companion object : Logger() {
//        const val COGNITO_GROUPS: String = "cognito:groups"
//        const val COGNITO_USERNAME: String = "username"
//        const val COGNITO_EMAIL: String = "email"
//    }
//
//    override fun doFilterInternal(
//        request: HttpServletRequest,
//        response: HttpServletResponse,
//        filterChain: FilterChain
//    ) {
//        if (isRequestURINotWhitelisted(request.requestURI)) {
//            val accessToken = getAccessTokenFromRequestHeader(request)
//            val username = jwtDecoder().decode(accessToken).claims[COGNITO_USERNAME] as String
//            if (!userContextService.existsUserProfileByUsername(username)) {
//                try {
//                    val userInfoJson = obtainUserInfo(accessToken)
//                    val email = userInfoJson.get(COGNITO_EMAIL).toString().removeSurrounding("\"")
//                    userContextService.createUserProfile(username, email)
//                } catch (e: RuntimeException) {
//                    throw AuthenticationException(e.message)
//                }
//            }
//        }
//        filterChain.doFilter(request, response)
//    }
//
//    private fun obtainUserInfo(accessToken: String): JsonNode {
//        val headers = HttpHeaders()
//        headers.set("Authorization", "Bearer $accessToken")
//        val res = rs.exchange("/oauth2/userInfo", HttpMethod.GET, HttpEntity<Any?>(headers), String::class.java)
//        val objectMapper = ObjectMapper()
//        return objectMapper.readTree(res.body)
//    }
//
//    private fun getAccessTokenFromRequestHeader(request: HttpServletRequest): String {
//        val bearerToken = request.getHeader("Authorization")
//        return bearerToken.takeIf { StringUtils.hasText(it) && bearerToken.startsWith("Bearer") }?.let {
//            bearerToken.substring(7)
//        } ?:  throw RuntimeException("Cannot read access token.")
//    }
//
//    private fun isRequestURINotWhitelisted(requestURI: String): Boolean {
//        return ! SecurityConfig.WHITELIST_PATHS.any { pathMatcher.match(it, requestURI) }
//    }
//
//    private fun jwtDecoder(): JwtDecoder {
//        val jwtDecoder: JwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
//        return JwtDecoder { token ->
//            val jwt: Jwt = jwtDecoder.decode(token)
//            jwt
//        }
//    }
//}
//
