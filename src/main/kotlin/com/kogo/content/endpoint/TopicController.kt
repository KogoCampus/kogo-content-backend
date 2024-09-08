package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ApiResponse
import com.kogo.content.endpoint.model.GroupDto
import com.kogo.content.service.TopicService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("media")
class TopicController @Autowired constructor(
    private val topicService : TopicService
) {
    @GetMapping("groups/{id}")
    fun getGroup(@PathVariable("id") groupId: String) = ApiResponse.success(topicService.find(groupId))

    // @GetMapping("groups")
    // fun searchGroupsByKeyword(
    //     @RequestParam(name = "q", defaultValue = "") query: String
    // ): ApiResponse {
    //     return ApiResponse.success(meiliSearchService.searchGroups(query))
    // }

    @RequestMapping(
        path = ["groups"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createGroup(@Valid groupDto: GroupDto) = ApiResponse.success(topicService.create(groupDto))

    @RequestMapping(
        path = ["groups/{id}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateGroup(
        @PathVariable("id") groupId: String,
        @RequestPart("groupName", required = false) groupName: String?,
        @RequestPart("description", required = false) description: String?,
        @RequestPart("tags", required = false) tags: String?,
        @RequestPart("profileImage", required = false) profileImage: MultipartFile?): ApiResponse {

        val attributes = mapOf(
            "groupName" to groupName,
            "description" to description,
            "tags" to tags,
            "profileImage" to profileImage
        ).filterValues { it != null }

        if (attributes.isEmpty())
            throw IllegalArgumentException("Empty request body")

        return ApiResponse.success(topicService.update(groupId, attributes))
    }

    @DeleteMapping("groups/{id}")
    fun deleteGroup(@PathVariable("id") groupId: String) = ApiResponse.success(topicService.delete(groupId))
}