package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.service.UserContextService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
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
    private val userContextService: UserContextService,
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
        HttpJsonResponse.successResponse(postService.listPostsByTopicId(topicId).map { buildPostResponse(it) })
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
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        HttpJsonResponse.successResponse(buildPostResponse(post))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = "application/json", schema = Schema(implementation = PostDto::class))])
    @Operation(
        summary = "create a post under the given topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun createPost(
        @PathVariable("topicId") topicId: String,
        @Valid postDto: PostDto) = run {
            findTopicByIdOrThrow(topicId)
            HttpJsonResponse.successResponse(buildPostResponse(postService.create(findTopicByIdOrThrow(topicId), userContextService.getCurrentUserContext(), postDto)))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}"],
        method = [RequestMethod.PUT],
        consumes = [org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = "application/json", schema = Schema(implementation = PostUpdate::class))])
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
        @Valid postUpdate: PostUpdate) = run {
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        HttpJsonResponse.successResponse(buildPostResponse(postService.update(post, postUpdate)))
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
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        HttpJsonResponse.successResponse(postService.delete(post))
    }

    private fun findTopicByIdOrThrow(topicId: String) = topicService.find(topicId) ?: throw ResourceNotFoundException.of<Topic>(topicId)

    private fun throwPostNotFound(postId: String): Nothing = throw ResourceNotFoundException.of<Post>(postId)

    private fun buildPostResponse(post: Post): PostResponse = with(post) {
        PostResponse(
            id = id!!,
            topicId = post.topic.id,
            authorUserId = author.id!!,
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

    private fun buildPostAttachmentResponse(attachment: Attachment): PostResponse.PostAttachment = with(attachment) {
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
