package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.logging.Logger
import com.kogo.content.service.entity.TopicService
import com.kogo.content.service.entity.UserContextService
import com.kogo.content.storage.entity.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class MeController @Autowired constructor(
    private val userService: UserContextService,
    private val topicService: TopicService,
) {
    companion object: Logger()

    @GetMapping("me")
    @Operation(
        summary = "get my user info",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )])
    fun getMe() = run {
        val me = userService.getCurrentUserDetails()
        HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(me))
    }

    @RequestMapping(
        path = ["me"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = UserUpdate::class))])
    @Operation(
        summary = "update my user info",
        requestBody = RequestBody(),
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData.IncludeCredentials::class))]
        )]
    )
    fun updateMe(
        @Valid meUpdate: UserUpdate) = run {
            val me = userService.getCurrentUserDetails()
            val updatedUser = userService.updateUserProfile(me, meUpdate)
            HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(updatedUser))
    }

    @GetMapping("me/ownership/posts")
    @Operation(
        summary = "get my posts",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)))],
        )])
    fun getPostsAuthoredByUser() = run {
        val me = userService.getCurrentUserDetails()
        HttpJsonResponse.successResponse(userService.getUserPosts(me).map { PostResponse.from(it) })
    }

    @GetMapping("me/ownership/topics")
    @Operation(
        summary = "get my topics",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = TopicResponse::class)))],
        )])
    fun getTopicsOwnedByUser() = run {
        val me = userService.getCurrentUserDetails()
        HttpJsonResponse.successResponse(userService.getUserTopics(me).map { TopicResponse.from(it) })
    }

    @RequestMapping(
        path = ["me/ownership/topics/{topicId}/transfer"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "transfer the topic ownership",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))]
        )]
    )
    fun transferOwner(
        @PathVariable("topicId") topicId: String,
        @RequestParam("transfer_to") newOwnerId: String): ResponseEntity<*> = run {
        val topic = topicService.find(topicId) ?: throwTopicNotFound(topicId)
        val newOwner = userService.find(newOwnerId) ?: throwUserNotFound(newOwnerId)
        val originalOwner = userService.getCurrentUserDetails()

        if(!topicService.isTopicOwner(topic, originalOwner))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the owner of this topic")

        if(originalOwner.id!! == newOwnerId)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "You cannot transfer ownership to yourself")
        val transferredTopic = topicService.transferOwnership(topic, newOwner)
        topicService.follow(topic, newOwner)

        HttpJsonResponse.successResponse(TopicResponse.from(transferredTopic))
    }

    @GetMapping("me/following")
    @Operation(
        summary = "get my following topics",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))]
        )])
    fun getMeFollowing() = run {
        val me = userService.getCurrentUserDetails()
        val followingTopics = topicService.listFollowingTopicsByUserId(me.id!!)
        HttpJsonResponse.successResponse(followingTopics.map{ TopicResponse.from(it) })
    }

    private fun throwUserNotFound(userId: String): Nothing = throw ResourceNotFoundException.of<UserDetails>(userId)
    private fun throwTopicNotFound(topicId: String): Nothing = throw ResourceNotFoundException.of<Topic>(topicId)
}

