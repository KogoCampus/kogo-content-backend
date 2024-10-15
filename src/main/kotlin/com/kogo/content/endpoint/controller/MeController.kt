package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.endpoint.model.UserResponse
import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.endpoint.model.TopicResponse
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
class MeController @Autowired constructor(
    private val userService : UserContextService
) {
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
        HttpJsonResponse.successResponse(userService.getUserFollowings(me).map { buildTopicResponse(it) })
    }

    private fun buildUserResponse(user: UserDetails) = with(user) {
        UserResponse(
            id = id!!,
            username = username,
            email = email,
            schoolName = schoolName,
            schoolShortenedName = schoolShortenedName,
            profileImage = profileImage?.let { buildUserProfileImage(it) },
            followingTopics = followingTopics,
        )
    }

    private fun buildUserProfileImage(attachment: Attachment) = with(attachment) {
        UserResponse.UserProfileImage(
            attachmentId = id!!,
            fileName = name,
            url = attachment.storeKey.toFileSourceUrl(),
            contentType = contentType,
            size = fileSize
        )
    }

    private fun buildTopicResponse(topic: Topic) = with(topic) {
        TopicResponse(
            id = id!!,
            ownerUserId = owner.id!!,
            topicName = topicName,
            description = description,
            tags = tags,
            profileImage = profileImage?.let { buildTopicProfileImage(it) },
            createdAt = createdAt!!,
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

    private fun buildPostResponse(post: Post): PostResponse = with(post) {
        PostResponse(
            id = id!!,
            topicId = post.topic.id,
            authorUserId = author.id!!,
            title = title,
            content = content,
            attachments = attachments.map { buildPostAttachmentResponse(it) },
            comments = emptyList(), // TODO
            viewcount = viewcount,
            likes = likes,
            createdAt = createdAt!!,
            commentCount = commentCount
        )
    }

    private fun buildPostAttachmentResponse(attachment: Attachment): PostResponse.PostAttachment = with(attachment) {
        PostResponse.PostAttachment(
            attachmentId = id,
            name = name,
            url = attachment.storeKey.toFileSourceUrl(),
            contentType = contentType,
            size = fileSize
        )
    }
}

