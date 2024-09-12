package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.Response
import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicResponse
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.logging.Logger
import com.kogo.content.service.AuthenticatedUserService
import com.kogo.content.service.TopicService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.TopicEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("media")
class TopicController @Autowired constructor(
    private val topicService : TopicService,
    private val authenticatedUserService: AuthenticatedUserService,
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
        val topic = topicService.find(topicId) ?: throw ResourceNotFoundException("Topic not found for id: $topicId")
        Response.success(buildTopicResponse(topic))
    }

    // @GetMapping("groups")
    // fun searchGroupsByKeyword(
    //     @RequestParam(name = "q", defaultValue = "") query: String
    // ): ApiResponse {
    //     return ApiResponse.success(meiliSearchService.searchGroups(query))
    // }

    @RequestMapping(
        path = ["topics"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "create a new topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun createTopic(@Valid topicDto: TopicDto): Response = run {
        if (topicService.findByTopicName(topicDto.topicName) != null)
            return Response.error(ErrorCode.BAD_REQUEST, "topic name must be unique: ${topicDto.topicName}")

        Response.success(buildTopicResponse(topicService.create(topicDto, authenticatedUserService.getCurrentAuthenticatedUser())))
    }

    @RequestMapping(
        path = ["topics/{id}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "update topic attributes",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun updateGroup(
        @PathVariable("id") topicId: String,
        @Valid topicUpdate: TopicUpdate): Response = run {
            val topic = topicService.find(topicId) ?: throw ResourceNotFoundException("Topic not found for id: $topicId")
            Response.success(buildTopicResponse(topicService.update(topic, topicUpdate)))
    }

    @DeleteMapping("topics/{id}")
    fun deleteTopic(@PathVariable("id") topicId: String) = run {
        val topic = topicService.find(topicId) ?: throw ResourceNotFoundException("Topic not found for id: $topicId")
        Response.success(topicService.delete(topic))
    }

    private fun buildTopicResponse(topic: TopicEntity) = with(topic) {
        TopicResponse(
            id = id!!,
            ownerId = owner?.id,
            topicName = topicName,
            description = description,
            tags = tags,
            profileImage = profileImage?.let { buildTopicProfileImage(it) }
        )
    }

    private fun buildTopicProfileImage(attachment: Attachment) = with(attachment) {
        TopicResponse.TopicProfileImage(
            attachmentId = id!!,
            fileName = fileName,
            url = savedLocationURL,
            contentType = contentType,
            size = fileSize
        )
    }
}
