package com.kogo.content.security

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kogo.content.logging.Logger
import com.kogo.content.service.UserService
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

@Component
class AuthenticationApiClient(
    private val userService: UserService,
    private val objectMapper: ObjectMapper
) {
    @Value("\${kogo-api.authenticate}")
    private lateinit var authenticationApiUrl: String

    private val restTemplate = RestTemplate()

    companion object : Logger() {
        private const val USERDATA = "userdata"
        private const val EMAIL = "email"
        private const val SCHOOL_KEY = "schoolKey"
        private const val SCHOOL_DATA = "schoolData"
        private const val SCHOOL_NAME = "name"
        private const val SCHOOL_SHORTENED_NAME = "shortenedName"
    }

    fun authenticateAndCreateUser(accessToken: String): User {
        val userInfoJson = authenticateWithExternalApi(accessToken).get(USERDATA)
        return createOrGetUser(userInfoJson)
    }

    private fun authenticateWithExternalApi(accessToken: String): JsonNode {
        val headers = HttpHeaders().apply { set("Authorization", "Bearer $accessToken") }
        val entity = HttpEntity<Any?>(headers)

        return try {
            val response = restTemplate.exchange(
                "$authenticationApiUrl?grant_type=access_token",
                HttpMethod.GET, entity, String::class.java
            )
            if (response.statusCode != HttpStatus.OK) {
                throw BadCredentialsException("External authentication failed with status code: ${response.statusCode.value()}")
            }
            objectMapper.readTree(response.body)
        } catch (ex: HttpServerErrorException) {
            throw RuntimeException("Failed to connect to the external authentication server - ${ex.statusCode} - ${ex.responseBodyAsString}")
        }
    }

    private fun createOrGetUser(userInfoJson: JsonNode): User {
        val email = userInfoJson.get(EMAIL).toString().removeSurrounding("\"")
        val schoolKey = userInfoJson.get(SCHOOL_KEY).toString().removeSurrounding("\"")
        val schoolDataJson = userInfoJson.get(SCHOOL_DATA)

        val schoolInfo = SchoolInfo(
            schoolKey = schoolKey,
            schoolName = schoolDataJson.get(SCHOOL_NAME).toString().removeSurrounding("\""),
            schoolShortenedName = schoolDataJson.get(SCHOOL_SHORTENED_NAME).toString().removeSurrounding("\"")
        )

        return userService.findUserByEmail(email) ?: userService.create(
            username = generateRandomUsername(),
            email = email,
            schoolInfo = schoolInfo
        )
    }

    private fun generateRandomUsername(): String {
        var username: String
        do {
            val randomString = (1..6)
                .map { ('a'..'z') + ('0'..'9') }
                .flatten()
                .shuffled()
                .take(6)
                .joinToString("")
            username = "#KogoUser$randomString"
        } while (userService.findUserByUsername(username) != null)

        return username
    }
}
