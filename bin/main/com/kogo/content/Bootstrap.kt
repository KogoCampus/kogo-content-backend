package com.kogo.content

import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.logging.Logger
import com.kogo.content.storage.model.entity.Follower
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
@Profile("!test")
class Bootstrap(private val mongoTemplate: MongoTemplate) {

    companion object : Logger() {
        private fun generateDocumentId(key: String): String {
            // Create a deterministic hex string based on the school key
            val baseString = "school_group_$key"
            val md5Bytes = java.security.MessageDigest.getInstance("MD5")
                .digest(baseString.toByteArray())

            val hexString = md5Bytes.take(12)
                .joinToString("") { "%02x".format(it) }

            return "60000000$hexString".take(24)
        }
    }

    @Value("\${kogo-api.getSchools}")
    lateinit var schoolsUrl: String

    private val restTemplate: RestTemplate = RestTemplate()

    data class SchoolsResponse(val schools: List<School>)
    data class School(
        val key: String,
        val emailDomains: List<String>,
        val name: String,
        val shortenedName: String
    )

    @EventListener(ApplicationReadyEvent::class)
    fun bootstrapData() {
        log.info { "Starting data bootstrap process..." }
        try {
            val systemUser = getOrCreateSystemUser()
            bootstrapSchoolGroups(systemUser)
            log.info { "Data bootstrap completed successfully" }
        } catch (e: Exception) {
            log.error(e) { "Failed to bootstrap data" }
            throw e
        }
    }

    private fun bootstrapSchoolGroups(systemUser: User) {
        log.info { "Fetching schools from $schoolsUrl" }
        val schoolsResponse = try {
            restTemplate.getForObject(schoolsUrl, SchoolsResponse::class.java)
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch schools from API" }
            return
        }

        log.info { "Retrieved ${schoolsResponse?.schools?.size} schools, starting group creation..." }

        schoolsResponse?.schools?.forEach { school ->
            val schoolGroupId = generateDocumentId(school.key)
            val existingGroup = mongoTemplate.findOne(
                Query.query(Criteria.where("_id").`is`(schoolGroupId)),
                Group::class.java
            )

            if (existingGroup == null) {
                try {
                    val schoolGroup = Group(
                        id = schoolGroupId,
                        groupName = school.name,
                        description = "Official group for ${school.name}",
                        tags = mutableListOf(school.shortenedName),
                        owner = systemUser,
                        isSchoolGroup = true,
                    )
                    schoolGroup.followers.add(Follower(systemUser))
                    val savedGroup = mongoTemplate.save(schoolGroup)
                    systemUser.followingGroupIds.add(savedGroup.id!!)

                    log.debug { "Created school group for: ${school.name} with ID: $schoolGroupId" }
                } catch (e: Exception) {
                    log.error(e) { "Failed to create school group for: ${school.name}" }
                }
            }
        }
    }

    private fun getOrCreateSystemUser(): User {
        val systemUserId = generateDocumentId("system_user")

        val systemUser = mongoTemplate.findOne(
            Query.query(Criteria.where("_id").`is`(systemUserId)),
            User::class.java
        )

        return if (systemUser != null) systemUser else {
            log.info { "Creating system user..." }
            try {
                mongoTemplate.save(
                    User(
                        id = systemUserId,
                        username = "system_user",
                        email = "op@sfu.ca",
                        schoolInfo = SchoolInfo(
                            schoolKey = "system",
                            schoolName = "System",
                            schoolShortenedName = null,
                        ),
                        followingGroupIds = mutableListOf()
                    )
                ).also { log.info { "System user created successfully" } }
            } catch (e: Exception) {
                log.error(e) { "Failed to create system user" }
                throw e
            }
        }
    }
}
