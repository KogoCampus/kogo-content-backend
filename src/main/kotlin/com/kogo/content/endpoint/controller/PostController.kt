package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.Response
import com.kogo.content.exception.IllegalPathVariableException
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.service.AuthenticatedUserService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.PostEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("media")
class PostController @Autowired constructor(
    private val postService : PostService,
    private val topicService : TopicService,
    private val authenticatedUserService: AuthenticatedUserService,
) {
    @GetMapping("topics/{topicId}/posts")
    @Operation(
        summary = "return a list of posts in the given topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)))],
        )])
    fun listPosts(@PathVariable("topicId") topicId: String) = run {
        findTopicByIdOrThrow(topicId)
        Response.success(postService.listPostsByTopicId(topicId).map { buildPostResponse(it) })
    }

    @GetMapping("topics/{topicId}/posts/{postId}")
    @Operation(
        summary = "return a post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun getPost(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String) = run {
        findTopicByIdOrThrow(topicId)
        val post = postService.find(postId) ?: throw ResourceNotFoundException("Post not found for id: $postId")
        Response.success(buildPostResponse(post))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @Operation(
        summary = "create a post under the given topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun createPost(
        @PathVariable("topicId") topicId: String,
        @Valid @RequestBody postDto: PostDto) = run {
            findTopicByIdOrThrow(topicId)
            Response.success(buildPostResponse(postService.create(findTopicByIdOrThrow(topicId), authenticatedUserService.getCurrentAuthenticatedUser(), postDto)))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}"],
        method = [RequestMethod.PUT],
        consumes = [org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @Operation(
        summary = "update attributes of an existing post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun updatePost(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @Valid @RequestBody postUpdate: PostUpdate): Response = run {
        val post = postService.find(postId) ?: throw ResourceNotFoundException("Post not found for id: $postId")
        Response.success(buildPostResponse(postService.update(post, postUpdate)))
    }

    @DeleteMapping("topics/{topicId}/posts/{postId}")
    @Operation(
        summary = "delete a post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
        )])
    fun deletePost(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String) = run {
        findTopicByIdOrThrow(topicId)
        val post = postService.find(postId) ?: throw ResourceNotFoundException("Post not found for id: $postId")
        Response.success(postService.delete(post))
    }

    fun findTopicByIdOrThrow(topicId: String) = topicService.find(topicId) ?: throw IllegalPathVariableException("Topic not found for id: $topicId")

    fun buildPostResponse(post: PostEntity): PostResponse = with(post) {
        PostResponse(
            id = id!!,
            authorId = author?.id,
            title = title,
            content = content,
            attachments = attachments.map { buildPostAttachmentResponse(it) },
            comments = emptyList(),
            viewcount = viewcount,
            likes = likes,
            viewed = viewed,
            liked = liked
        )
    }

    fun buildPostAttachmentResponse(attachment: Attachment): PostResponse.PostAttachment = with(attachment) {
        PostResponse.PostAttachment(
            attachmentId = id!!,
            fileName = fileName,
            url = savedLocationURL,
            contentType = contentType,
            size = fileSize
        )
    }

    // TODO
    // fun buildPostCommentResponse(comment)
}
