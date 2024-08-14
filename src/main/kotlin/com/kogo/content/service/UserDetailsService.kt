package com.kogo.content.service

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

@Service
class UserDetailsService : UserDetailsService {

    fun getUserAttributes(): Map<String, Any> {
        val authentication: Authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal
        val claims: Map<String, Any> = (principal as Jwt).claims
        /**
         *     if (claims.containsKey(COGNITO_GROUPS))
         *       ((Map<String, Object>) claims).put(SPRING_AUTHORITIES, claims.get(COGNITO_GROUPS));
         *     if (claims.containsKey(COGNITO_USERNAME))
         *       ((Map<String, Object>) claims).put(SPRING_USER_NAME, claims.get(COGNITO_USERNAME));
         */
        return claims
    }

    override fun loadUserByUsername(username: String?): UserDetails
        = throw UnsupportedOperationException("unsupported")
}