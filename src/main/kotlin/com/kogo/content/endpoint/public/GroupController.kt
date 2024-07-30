package com.kogo.content.endpoint.public

import com.kogo.content.endpoint.public.model.ApiResponse
import com.kogo.content.endpoint.public.model.GroupDto
import com.kogo.content.service.EntityService
import com.kogo.content.storage.entity.GroupEntity
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("media")
class GroupController @Autowired constructor(
    private val groupService : EntityService<GroupEntity, GroupDto>
) {
    @GetMapping("groups/{id}")
    fun getGroup(@PathVariable("id") groupId: String) = ApiResponse.success(groupService.find(groupId))

    @RequestMapping(
        path = ["groups"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createGroup(@Valid groupDto: GroupDto) = ApiResponse.success(groupService.create(groupDto))

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

        return ApiResponse.success(groupService.update(groupId, attributes))
    }

    @DeleteMapping("groups/{id}")
    fun deleteGroup(@PathVariable("id") groupId: String) = ApiResponse.success(groupService.delete(groupId))
}