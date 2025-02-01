package com.kogo.content.service

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class CourseListingService {
    @Value("\${kogo-api.courseListings}")
    lateinit var courseListingsBaseUrl: String

    private val restTemplate = RestTemplate()

    data class CourseListing(
        val schoolKey: String = "",
        val semester: String,
        val programs: JsonNode,
    ) {
        fun getCourse(courseCodeBase64: String): JsonNode {
            val decodedCourseCode = String(java.util.Base64.getDecoder().decode(courseCodeBase64))
            return programs.flatMap { it["courses"].asIterable() }
                .find { it["courseCode"].asText() == decodedCourseCode } ?: throw RuntimeException("course $decodedCourseCode not found $schoolKey")
        }
    }

    fun retrieveCourseListing(schoolKey: String): CourseListing {
        val url = "${courseListingsBaseUrl}/$schoolKey"
        return restTemplate.getForObject(url, CourseListing::class.java)
            ?: throw RuntimeException("Failed to retrieve course listing for school: $schoolKey")
    }
}
