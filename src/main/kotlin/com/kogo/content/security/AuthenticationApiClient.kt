package com.kogo.content.security

import com.kogo.content.logging.Logger
import com.kogo.content.service.UserService
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.model.entity.Group
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
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
    private val mongoTemplate: MongoTemplate
) {
    data class AuthenticationResponse(
        val userdata: UserData
    )

    data class UserData(
        val email: String,
        val schoolKey: String,
        val schoolData: SchoolData
    )

    data class SchoolData(
        val emailDomains: List<String>,
        val name: String,
        val shortenedName: String
    )

    @Value("\${kogo-api.authenticate}")
    private lateinit var authenticationApiUrl: String

    private val restTemplate = RestTemplate()

    companion object : Logger()

    fun authenticateAndCreateUser(accessToken: String): User {
        val authResponse = authenticateWithExternalApi(accessToken)
        return createOrGetUser(authResponse.userdata)
    }

    private fun authenticateWithExternalApi(accessToken: String): AuthenticationResponse {
        val headers = HttpHeaders().apply { set("Authorization", "Bearer $accessToken") }
        val entity = HttpEntity<Any?>(headers)

        return try {
            val response = restTemplate.exchange(
                "$authenticationApiUrl?grant_type=access_token",
                HttpMethod.GET, entity, AuthenticationResponse::class.java
            )
            if (response.statusCode != HttpStatus.OK) {
                throw BadCredentialsException("External authentication failed with status code: ${response.statusCode.value()}")
            }
            response.body ?: throw BadCredentialsException("Empty response from authentication server")
        } catch (ex: HttpServerErrorException) {
            throw RuntimeException("Failed to connect to the external authentication server - ${ex.statusCode} - ${ex.responseBodyAsString}")
        }
    }

    private fun createOrGetUser(userData: UserData): User {
        val existingUser = userService.findUserByEmail(userData.email)
        if (existingUser != null) {
            return existingUser
        }

        val schoolInfo = SchoolInfo(
            schoolKey = userData.schoolKey,
            schoolName = userData.schoolData.name,
            schoolShortenedName = userData.schoolData.shortenedName
        )

        val newUser = userService.create(
            username = generateRandomUsername(),
            email = userData.email,
            schoolInfo = schoolInfo
        )

        // Join school group if it exists
        joinSchoolGroup(newUser, userData.schoolKey)

        return newUser
    }

    private fun joinSchoolGroup(user: User, schoolKey: String) {
        log.info { "Attempting to join school group for user: ${user.email}" }

        val schoolGroup = mongoTemplate.findOne(
            Query.query(Criteria.where("id").`is`(schoolKey)),
            Group::class.java
        )

        if (schoolGroup != null) {
            try {
                // Add user to group followers
                if (!schoolGroup.followerIds.contains(user.id)) {
                    schoolGroup.followerIds.add(user.id!!)
                    mongoTemplate.save(schoolGroup)
                }
                if (!user.followingGroupIds.contains(schoolGroup.id)) {
                    user.followingGroupIds.add(schoolGroup.id!!)
                    mongoTemplate.save(user)
                }

                log.info { "Successfully joined school group ${schoolGroup.groupName} for user: ${user.email}" }
            } catch (e: Exception) {
                log.error(e) { "Failed to join school group for user: ${user.email}" }
            }
        } else {
            log.warn { "School group not found for key: $schoolKey" }
        }
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
