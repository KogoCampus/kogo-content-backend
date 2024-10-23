package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.OwnerInfoResponse
import com.kogo.content.endpoint.model.AttachmentResponse
import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicResponse
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.exception.UserIsNotOwnerException
import com.kogo.content.logging.Logger
import com.kogo.content.searchengine.Document
import com.kogo.content.searchengine.SearchIndex
import com.kogo.content.searchengine.SearchIndexService
import com.kogo.content.service.UserContextService
import com.kogo.content.service.TopicService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
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
    fun updateGroup(
        @PathVariable("id") topicId: String,
        @Valid topicUpdate: TopicUpdate): ResponseEntity<*> = run {
            val topic = topicService.find(topicId) ?: throwTopicNotFound(topicId)
            if(!topicService.isTopicOwner(topic, userContextService.getCurrentUserDetails()))
                throwUserIsNotOwner(topicId)
            if (topicUpdate.topicName != null && topicService.existsByTopicName(topicUpdate.topicName!!))
                return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "topic name must be unique: ${topicUpdate.topicName}")
            val updatedTopic = topicService.update(topic, topicUpdate)
            val updatedTopicDocument = buildTopicIndexDocumentUpdate(topicId, topicUpdate)
            searchIndexService.updateDocument(SearchIndex.TOPICS, updatedTopicDocument)
            HttpJsonResponse.successResponse(buildTopicResponse(updatedTopic))
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
    fun deleteTopic(@PathVariable("id") topicId: String) = run {
        val topic = topicService.find(topicId) ?: throwTopicNotFound(topicId)
        if(!topicService.isTopicOwner(topic, userContextService.getCurrentUserDetails()))
            throwUserIsNotOwner(topicId)
        val deletedTopic = topicService.delete(topic)
        searchIndexService.deleteDocument(SearchIndex.TOPICS, topicId)
        HttpJsonResponse.successResponse(deletedTopic)
    }

    @RequestMapping(
        path = ["topics/{id}/follow"],
        method = [RequestMethod.POST]
    )
    @Operation(
        summary = "follow a topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun followTopic(@PathVariable("id") topicId: String): ResponseEntity<*> = run {
        val topic = topicService.find(topicId) ?: throwTopicNotFound(topicId)
        val user = userContextService.getCurrentUserDetails()
        if (topicService.existsFollowingByUserIdAndTopicId(user.id!!, topic.id!!))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The user is already following the topic")
        topicService.follow(topic, user)
        HttpJsonResponse.successResponse(buildTopicResponse(topic))
    }

    @RequestMapping(
        path = ["topics/{id}/unfollow"],
        method = [RequestMethod.POST]
    )
    @Operation(
        summary = "unfollow a topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun unfollowTopic(@PathVariable("id") topicId: String): ResponseEntity<*> = run {
        val topic = topicService.find(topicId) ?: throwTopicNotFound(topicId)
        val user = userContextService.getCurrentUserDetails()
        if(topicService.isTopicOwner(topic, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The owner cannot unfollow the topic")
        if (!topicService.existsFollowingByUserIdAndTopicId(user.id!!, topic.id!!))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The user is not following the topic")
        topicService.unfollow(topic, user)
        HttpJsonResponse.successResponse(buildTopicResponse(topic))
    }

    private fun buildTopicResponse(topic: Topic): TopicResponse = with(topic) {
        TopicResponse(
            id = id!!,
            owner = buildOwnerInfoResponse(owner),
            topicName = topicName,
            description = description,
            tags = tags,
            profileImage = profileImage?.let { buildAttachmentResponse(it) },
            createdAt = createdAt!!,
            updatedAt = updatedAt!!,
            userCount = userCount
        )
    }

    private fun buildOwnerInfoResponse(owner: UserDetails): OwnerInfoResponse = with(owner) {
        OwnerInfoResponse(
            ownerId = id,
            username = username,
            profileImage = profileImage?.let { buildAttachmentResponse(it) },
            schoolShortenedName = schoolShortenedName
        )
    }

    private fun buildAttachmentResponse(attachment: Attachment): AttachmentResponse = with(attachment) {
        AttachmentResponse(
            attachmentId = id!!,
            name = name,
            url = attachment.storeKey.toFileSourceUrl(),
            contentType = contentType,
            size = fileSize
        )
    }

    private fun buildTopicIndexDocument(topic: Topic): Document {
        val timestamp = topic.createdAt?.epochSecond
        return Document(topic.id!!).apply {
            put("topicName", topic.topicName)
            put("description", topic.description)
            put("ownerId", topic.owner.id!!)
            put("tags", topic.tags)
            put("createdAt", timestamp!!)
        }
    }

    private fun buildTopicIndexDocumentUpdate(topicId: String, topicUpdate: TopicUpdate): Document{
        return Document(topicId).apply {
            topicUpdate.topicName?.let { put("topicName", it) }
            topicUpdate.description?.let { put("description", it) }
            topicUpdate.tags?.let { put("tags", it) }
        }
    }

    private fun throwTopicNotFound(topicId: String): Nothing = throw ResourceNotFoundException.of<Topic>(topicId)

    private fun throwUserIsNotOwner(topicId: String): Nothing = throw UserIsNotOwnerException.of<Topic>(topicId)
}
