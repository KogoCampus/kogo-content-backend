package com.kogo.content

import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.logging.Logger
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.Instant

@Service
@Profile("local || stg || prd")
class DataBootstrap(private val mongoTemplate: MongoTemplate) {

    @Value("\${kogo-api.getSchools}")
    lateinit var schoolsUrl: String

    private val restTemplate: RestTemplate = RestTemplate()

    companion object : Logger()

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

        var createdCount = 0
        var existingCount = 0

        schoolsResponse?.schools?.forEach { school ->
            val existingGroup = mongoTemplate.findOne(
                Query.query(Criteria.where("id").`is`(school.key)),
                Group::class.java
            )

            if (existingGroup == null) {
                try {
                    val schoolGroup = Group(
                        id = ObjectId(school.key).toString(),
                        groupName = school.name,
                        description = "Official group for ${school.name}",
                        tags = mutableListOf(school.shortenedName),
                        owner = systemUser,
                        isSchoolGroup = true,
                        followerIds = mutableListOf(systemUser.id!!),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                    mongoTemplate.save(schoolGroup)
                    systemUser.followingGroupIds.add(school.key)
                    mongoTemplate.save(systemUser)
                    createdCount++

                    log.debug { "Created school group for: ${school.name} and added system user as follower" }
                } catch (e: Exception) {
                    log.error(e) { "Failed to create school group for: ${school.name}" }
                }
            } else {
                existingCount++
                log.debug { "School group already exists for: ${school.name}" }
            }
        }
    }

    private fun getOrCreateSystemUser(): User {
        log.info { "Checking for system user..." }
        val systemUser = mongoTemplate.findOne(
            Query.query(Criteria.where("id").`is`("system")),
            User::class.java
        )

        return if (systemUser != null) {
            log.info { "System user already exists" }
            systemUser
        } else {
            log.info { "Creating system user..." }
            try {
                mongoTemplate.save(
                    User(
                        id = ObjectId("system").toString(),
                        username = "system",
                        email = "system@kogocampus.com",
                        schoolInfo = SchoolInfo(
                            schoolKey = "system",
                            schoolName = "System",
                            schoolShortenedName = "SYS"
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
