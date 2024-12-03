package com.kogo.content.endpoint.controller

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.logging.Logger
import com.kogo.content.service.NotificationService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserService
import com.kogo.content.storage.entity.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
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
    private val userService: UserService,
    private val topicService: TopicService,
    private val postService: PostService,
    private val notificationService: NotificationService
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
        val me = userService.getCurrentUser()
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
            val me = userService.getCurrentUser()
            val updatedUser = userService.updateUser(me, meUpdate)
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
        val me = userService.getCurrentUser()
        HttpJsonResponse.successResponse(postService.findPostsByAuthor(me).map { PostResponse.create(postService.findAggregate(it.id!!), me) })
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
        val me = userService.getCurrentUser()
        HttpJsonResponse.successResponse(topicService.findTopicsByOwner(me).map { TopicResponse.create(topicService.findAggregate(it.id!!), me) })
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
        val originalOwner = userService.getCurrentUser()

        if(!topicService.isUserTopicOwner(topic, originalOwner))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the owner of this topic")

        if(originalOwner.id!! == newOwnerId)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "You cannot transfer ownership to yourself")
        val transferredTopic = topicService.transferOwnership(topic, newOwner)
        topicService.follow(topic, newOwner)

        HttpJsonResponse.successResponse(TopicResponse.create(topicService.findAggregate(transferredTopic.id!!), originalOwner))
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
        val me = userService.getCurrentUser()
        val followingTopics = topicService.getAllFollowingTopicsByUserId(me.id!!)
        HttpJsonResponse.successResponse(followingTopics.map{ TopicResponse.create(topicService.findAggregate(it.id!!), me) })
    }

    @RequestMapping(
        path = ["me/push-token"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "update my push token",
        parameters = [
            Parameter(
                name = "push_token",
                description = "Push token",
                required = true,
                schema = Schema(type = "string")
            )],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserData::class))]
        )])
    fun updatePushToken(
        @RequestParam("push_token") pushToken: String
    ): ResponseEntity<*> = run {
        val me = userService.getCurrentUser()
        val updatedMe = notificationService.updatePushToken(me.id!!, pushToken)
        HttpJsonResponse.successResponse(UserData.IncludeCredentials.from(updatedMe))
    }

    @GetMapping(path = ["me/notifications"])
    @Operation(
        summary = "get notifications whose recipient is me",
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
                schema = Schema(implementation = Notification::class)
            ))],
        )]
    ) fun sendNotification(
        paginationRequest: PaginationRequest
    ): ResponseEntity<*> = run {
        val me = userService.getCurrentUser()
        val paginationResponse = notificationService.getNotificationsByRecipientEmail(me.email!!, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items,
            headers = paginationResponse.toHttpHeaders()
        )
    }

    private fun throwUserNotFound(userId: String): Nothing = throw ResourceNotFoundException.of<User>(userId)
    private fun throwTopicNotFound(topicId: String): Nothing = throw ResourceNotFoundException.of<Topic>(topicId)
}

