package com.kogo.content.endpoint.controller

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.logging.Logger
import com.kogo.content.service.PostService
import com.kogo.content.service.UserService
import com.kogo.content.service.TopicService
import com.kogo.content.storage.entity.Notification
import com.kogo.content.storage.entity.Topic
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
class TopicController @Autowired constructor(
    private val topicService : TopicService,
    private val postService: PostService,
    private val userService: UserService,
) {
    companion object : Logger()

    @GetMapping("topics/{id}")
    @Operation(
        summary = "return a topic info",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun getTopic(@PathVariable("id") topicId: String) = run {
        val topic = findTopicByIdOrThrow(topicId)

        HttpJsonResponse.successResponse(TopicResponse.create(topicService.findAggregate(topic.id!!), userService.getCurrentUser()))
    }

        @GetMapping("topics")
    @Operation(
        summary = "return a list of topics",
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
                schema = Schema(implementation = TopicResponse::class))
            )],
        )])
    fun listTopics(paginationRequest: PaginationRequest): ResponseEntity<*> = run {
        val user = userService.getCurrentUser()
        val paginationResponse = topicService.getAllTopics(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { TopicResponse.create(it, user) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @RequestMapping(
        path = ["topics"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = TopicDto::class))])
    @Operation(
        summary = "create a new topic",
        requestBody = RequestBody(),
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun createTopic(@Valid topicDto: TopicDto): ResponseEntity<*> = run {
        if (topicService.findTopicByTopicName(topicDto.topicName) != null) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "topic name must be unique: ${topicDto.topicName}")
        }
        val user = userService.getCurrentUser()
        val topic = topicService.create(topicDto, userService.getCurrentUser())
        topicService.follow(topic, user)
        HttpJsonResponse.successResponse(TopicResponse.create(topicService.findAggregate(topic.id!!), user))
    }

    @RequestMapping(
        path = ["topics/{id}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = TopicUpdate::class))])
    @Operation(
        summary = "update topic attributes",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "ok",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(mediaType = "application/json", schema = Schema(example = "{ \"reason\": \"USER_IS_NOT_OWNER\"}"))]
            )
        ])
    fun updateTopic(
        @PathVariable("id") topicId: String,
        @Valid topicUpdate: TopicUpdate): ResponseEntity<*> = run {
            val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
            val user = userService.getCurrentUser()

            if(!topicService.isUserTopicOwner(topic, userService.getCurrentUser()))
                return HttpJsonResponse.errorResponse(ErrorCode.USER_ACTION_DENIED, "topic is not owned by user ${user.id}")

            if (topicUpdate.topicName != null && topicService.findTopicByTopicName(topicUpdate.topicName!!) != null) {
                return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "topic name must be unique: ${topicUpdate.topicName}")
            }
            val updatedTopic = topicService.update(topic, topicUpdate)
            HttpJsonResponse.successResponse(TopicResponse.create(topicService.findAggregate(updatedTopic.id!!), user))
    }

    @DeleteMapping("topics/{id}")
    @Operation(
        summary = "delete a topic",
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
    fun deleteTopic(@PathVariable("id") topicId: String): ResponseEntity<*> {
        val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
        val user = userService.getCurrentUser()

        if(!topicService.isUserTopicOwner(topic, user))
           return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "topic is not owned by user ${user.id}")

        val deletedTopic = topicService.delete(topic)
        return HttpJsonResponse.successResponse(deletedTopic)
    }

    @RequestMapping(
        path = ["topics/{id}/follow"],
        method = [RequestMethod.PUT]
    )
    @Operation(
        summary = "follow a topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun followTopic(@PathVariable("id") topicId: String): ResponseEntity<*> = run {
        val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
        val user = userService.getCurrentUser()

        if (topicService.hasUserFollowedTopic(topic, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The user is already following the topic")

        topicService.follow(topic, user)
        HttpJsonResponse.successResponse(TopicResponse.create(topicService.findAggregate(topic.id!!), user), "User's follow added successfully to topic: $topicId")
    }

    @RequestMapping(
        path = ["topics/{id}/unfollow"],
        method = [RequestMethod.PUT]
    )
    @Operation(
        summary = "unfollow a topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun unfollowTopic(@PathVariable("id") topicId: String): ResponseEntity<*> = run {
        val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
        val user = userService.getCurrentUser()

        if(topicService.isUserTopicOwner(topic, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The owner cannot unfollow the topic")

        if (!topicService.hasUserFollowedTopic(topic, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The user is not following the topic")

        topicService.unfollow(topic, user)
        HttpJsonResponse.successResponse(TopicResponse.create(topicService.findAggregate(topic.id!!), user), "User's follow successfully removed from topic: $topicId")
    }

    @GetMapping(path = ["topics/{id}/users"])
    @Operation(
        summary = "get users following a given topic",
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
    ) fun listUsersFollowingTopic(
        @PathVariable("id") topicId: String,
        paginationRequest: PaginationRequest
    ): ResponseEntity<*> = run {
        findTopicByIdOrThrow(topicId)

        val paginationResponse = userService.getAllUsersFollowingTopic(topicId, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { UserData.Public.from(it) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    private fun findTopicByIdOrThrow(topicId: String) = topicService.find(topicId) ?: throw ResourceNotFoundException.of<Topic>(topicId)
}
