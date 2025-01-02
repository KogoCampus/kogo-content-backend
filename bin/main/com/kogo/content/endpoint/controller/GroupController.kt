package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.logging.Logger
import com.kogo.content.service.UserService
import com.kogo.content.service.GroupService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("media")
class GroupController @Autowired constructor(
    private val groupService : GroupService,
    private val userService: UserService,
) {
    companion object : Logger()

    @GetMapping("groups/{id}")
    @Operation(
        summary = "return a group info",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupResponse::class))],
        )])
    fun getGroup(@PathVariable("id") groupId: String) = run {
        val group = groupService.findOrThrow(groupId)

        HttpJsonResponse.successResponse(GroupResponse.from(group, userService.findCurrentUser()))
    }

    @GetMapping("groups")
    @Operation(
        summary = "return a list of groups",
        parameters = [
            Parameter(
                name = PaginationRequest.PAGE_TOKEN_PARAM,
                description = "page token",
                schema = Schema(type = "string"),
                required = false
            ),
            Parameter(
                name = PaginationRequest.PAGE_SIZE_PARAM,
                description = "limit for pagination",
                schema = Schema(type = "integer", defaultValue = "10"),
                required = false
            )],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
                Header(name = PaginationSlice.HEADER_PAGE_SIZE, schema = Schema(type = "string")),
            ],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = GroupResponse::class))
            )],
        )])
    fun listGroups(paginationRequest: PaginationRequest): ResponseEntity<*> = run {
        val user = userService.findCurrentUser()
        val paginationResponse = groupService.findAll(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { GroupResponse.from(it, user) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @RequestMapping(
        path = ["groups"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = GroupDto::class))])
    @Operation(
        summary = "create a new group",
        requestBody = RequestBody(),
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupResponse::class))],
        )])
    fun createGroup(@Valid groupDto: GroupDto): ResponseEntity<*> = run {
        if (groupService.findByGroupName(groupDto.groupName) != null) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "group name must be unique: ${groupDto.groupName}")
        }
        val user = userService.findCurrentUser()
        val group = groupService.create(groupDto, owner = userService.findCurrentUser())
        groupService.follow(group, user)
        HttpJsonResponse.successResponse(GroupResponse.from(group, user))
    }

    @RequestMapping(
        path = ["groups/{id}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = GroupUpdate::class))])
    @Operation(
        summary = "update group attributes",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "ok",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(mediaType = "application/json", schema = Schema(example = "{ \"reason\": \"USER_IS_NOT_OWNER\"}"))]
            )
        ])
    fun updateGroup(
        @PathVariable("id") groupId: String,
        @Valid groupUpdate: GroupUpdate): ResponseEntity<*> = run {
            val group = groupService.find(groupId) ?: groupService.findOrThrow(groupId)
            val user = userService.findCurrentUser()

            if(group.owner.id != user.id)
                return HttpJsonResponse.errorResponse(ErrorCode.USER_ACTION_DENIED, "group is not owned by user ${user.id}")

            if (groupUpdate.groupName != null 
                    && groupUpdate.groupName != group.name 
                    && groupService.findByGroupName(groupUpdate.groupName!!) != null) {
                return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "group name must be unique: ${groupUpdate.groupName}")
            }
            val updatedGroup = groupService.update(group, groupUpdate)
            HttpJsonResponse.successResponse(GroupResponse.from(updatedGroup, user))
    }

    @DeleteMapping("groups/{id}")
    @Operation(
        summary = "delete a group",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "ok",
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(mediaType = "application/json", schema = Schema(example = "{ \"reason\": \"USER_IS_NOT_OWNER\"}"))]
            )
        ])
    fun deleteGroup(@PathVariable("id") groupId: String): ResponseEntity<*> {
        val group = groupService.find(groupId) ?: groupService.findOrThrow(groupId)
        val user = userService.findCurrentUser()

        if(group.owner.id != user.id)
           return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "group is not owned by user ${user.id}")

        val deletedTopic = groupService.delete(group)
        return HttpJsonResponse.successResponse(deletedTopic)
    }

    @RequestMapping(
        path = ["groups/{id}/follow"],
        method = [RequestMethod.PUT]
    )
    @Operation(
        summary = "follow a group",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupResponse::class))],
        )])
    fun followGroup(@PathVariable("id") groupId: String): ResponseEntity<*> = run {
        val group = groupService.find(groupId) ?: groupService.findOrThrow(groupId)
        val user = userService.findCurrentUser()

        if (group.followerIds.contains(user.id))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The user is already following the group")

        groupService.follow(group, user)
        HttpJsonResponse.successResponse(GroupResponse.from(group, user), "User's follow added successfully to group: $groupId")
    }

    @RequestMapping(
        path = ["groups/{id}/unfollow"],
        method = [RequestMethod.PUT]
    )
    @Operation(
        summary = "unfollow a group",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupResponse::class))],
        )])
    fun unfollowGroup(@PathVariable("id") groupId: String): ResponseEntity<*> = run {
        val group = groupService.find(groupId) ?: groupService.findOrThrow(groupId)
        val user = userService.findCurrentUser()

        if(group.owner.id != user.id)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The owner cannot unfollow the group")

        if (!group.followerIds.contains(user.id))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The user is not following the group")

        groupService.unfollow(group, user)
        HttpJsonResponse.successResponse(GroupResponse.from(group, user), "User's follow successfully removed from group: $groupId")
    }
}
