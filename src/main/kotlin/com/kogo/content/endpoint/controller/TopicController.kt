package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicResponse
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.logging.Logger
import com.kogo.content.searchengine.Document
import com.kogo.content.searchengine.SearchIndex
import com.kogo.content.searchengine.SearchIndexService
import com.kogo.content.service.UserContextService
import com.kogo.content.service.TopicService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Topic
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("media")
class TopicController @Autowired constructor(
    private val topicService : TopicService,
    private val userContextService: UserContextService,
    private val searchIndexService: SearchIndexService
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
        val topic = topicService.find(topicId) ?: throwTopicNotFound(topicId)
        HttpJsonResponse.successResponse(buildTopicResponse(topic))
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
        if (topicService.existsByTopicName(topicDto.topicName))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "topic name must be unique: ${topicDto.topicName}")
        val topic = topicService.create(topicDto, userContextService.getCurrentUserDetails())
        val topicDocument = buildTopicIndexDocument(topic)
        searchIndexService.addDocument(SearchIndex.TOPICS, topicDocument)
        HttpJsonResponse.successResponse(buildTopicResponse(topic))
    }

    @RequestMapping(
        path = ["topics/{id}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = TopicUpdate::class))])
    @Operation(
        summary = "update topic attributes",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun updateGroup(
        @PathVariable("id") topicId: String,
        @Valid topicUpdate: TopicUpdate) = run {
            val topic = topicService.find(topicId) ?: throwTopicNotFound(topicId)
            HttpJsonResponse.successResponse(buildTopicResponse(topicService.update(topic, topicUpdate)))
    }

    @DeleteMapping("topics/{id}")
    @Operation(
        summary = "delete a topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
        )])
    fun deleteTopic(@PathVariable("id") topicId: String) = run {
        val topic = topicService.find(topicId) ?: throwTopicNotFound(topicId)
        HttpJsonResponse.successResponse(topicService.delete(topic))
    }

    private fun buildTopicResponse(topic: Topic) = with(topic) {
        TopicResponse(
            id = id!!,
            ownerUserId = owner.id!!,
            topicName = topicName,
            description = description,
            tags = tags,
            profileImage = profileImage?.let { buildTopicProfileImage(it) }
        )
    }

    private fun buildTopicProfileImage(attachment: Attachment) = with(attachment) {
        TopicResponse.TopicProfileImage(
            attachmentId = id!!,
            name = name,
            url = attachment.storeKey.toFileSourceUrl(),
            contentType = contentType,
            size = fileSize
        )
    }

    private fun buildTopicIndexDocument(topic: Topic): Document {
        return Document(topic.id!!).apply {
            put("topicName", topic.topicName)
            put("description", topic.description)
            put("ownerId", topic.owner.id!!)
            put("tags", topic.tags)
        }
    }

    private fun throwTopicNotFound(topicId: String): Nothing = throw ResourceNotFoundException.of<Topic>(topicId)
}
