package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.exception.UserIsNotOwnerException
import com.kogo.content.logging.Logger
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.FollowingTopicRepository
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
class MeController @Autowired constructor(
    private val userService: UserContextService,
    private val topicService: TopicService,
    private val followingTopicRepository: FollowingTopicRepository
) {
    companion object: Logger()

    @GetMapping("me")
    @Operation(
        summary = "get my user info",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserResponse::class))]
        )])
    fun getMe() = run {
        val me = userService.getCurrentUserDetails()
        HttpJsonResponse.successResponse(buildUserResponse(me))
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
            content = [Content(mediaType = "application/json", schema = Schema(implementation = UserResponse::class))]
        )]
    )
    fun updateMe(
        @Valid meUpdate: UserUpdate) = run {
            val me = userService.getCurrentUserDetails()
            HttpJsonResponse.successResponse(buildUserResponse(userService.updateUserProfile(me, meUpdate)))
    }

    @GetMapping("me/ownership/posts")
    @Operation(
        summary = "get my posts",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))]
        )])
    fun getMePosts() = run {
        val me = userService.getCurrentUserDetails()
        HttpJsonResponse.successResponse(userService.getUserPosts(me).map { buildPostResponse(it) })
    }

    @GetMapping("me/ownership/topics")
    @Operation(
        summary = "get my topics",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))]
        )])
    fun getMeGroups() = run {
        val me = userService.getCurrentUserDetails()
        HttpJsonResponse.successResponse(userService.getUserTopics(me).map { buildTopicResponse(it) })
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
            throwUserIsNotOwner(topicId)
        if(originalOwner.id!! == newOwnerId)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "You cannot transfer ownership to yourself")
        val transferredTopic = topicService.transfer0wnership(topic, newOwner)
        topicService.follow(topic, newOwner)
        HttpJsonResponse.successResponse(buildTopicResponse(transferredTopic))
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
        val followingTopics = topicService.findFollowingByOwnerId(me.id!!)
        HttpJsonResponse.successResponse(followingTopics.map{ buildTopicResponse(it) })
    }

    private fun throwUserNotFound(userId: String): Nothing = throw ResourceNotFoundException.of<UserDetails>(userId)
    private fun throwTopicNotFound(topicId: String): Nothing = throw ResourceNotFoundException.of<Topic>(topicId)
    private fun throwUserIsNotOwner(topicId: String): Nothing = throw UserIsNotOwnerException.of<Topic>(topicId)

    private fun buildUserResponse(user: UserDetails) = with(user) {
        UserResponse(
            id = id!!,
            username = username,
            email = email,
            schoolName = schoolName,
            schoolShortenedName = schoolShortenedName,
            profileImage = profileImage?.let { buildAttachmentResponse(it) },
        )
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
        )
    }

    private fun buildPostResponse(post: Post): PostResponse = with(post) {
        PostResponse(
            id = id!!,
            topicId = post.topic.id,
            owner = buildOwnerInfoResponse(owner),
            title = title,
            content = content,
            attachments = attachments.map { buildAttachmentResponse(it) },
            comments = comments.map { buildPostComment(it) },
            viewcount = viewcount,
            likes = likes,
            createdAt = createdAt!!,
            updatedAt = updatedAt!!,
            commentCount = commentCount,
        )
    }

    private fun buildOwnerInfoResponse(owner: UserDetails): OwnerInfoResponse =  with(owner) {
        OwnerInfoResponse(
            ownerId = id,
            username = username,
            profileImage = profileImage?.let { buildAttachmentResponse(it) },
            schoolShortenedName = schoolShortenedName
        )
    }

    private fun buildAttachmentResponse(attachment: Attachment): AttachmentResponse = with(attachment) {
        AttachmentResponse(
            attachmentId = id,
            name = name,
            url = attachment.storeKey.toFileSourceUrl(),
            contentType = contentType,
            size = fileSize
        )
    }

    private fun buildPostComment(comment: Comment): PostResponse.PostComment = with(comment) {
        PostResponse.PostComment(
            commentId = id,
            ownerId = buildOwnerInfoResponse(owner),
            replyCount = repliesCount
        )
    }
}

