package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.PaginationRequest
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.service.UserContextService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
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
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
        parameters = [Parameter(schema = Schema(implementation = PaginationRequest::class))],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [Header(name = "next_page", schema = Schema(type = "string"))],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)))],
        )])
    fun listPostsInTopic(
        @PathVariable("topicId") topicId: String,
        @RequestParam("limit") limit: Int?,
        @RequestParam("page") page: String?) = run {
        findTopicByIdOrThrow(topicId)
        val paginationRequest = if (limit != null) PaginationRequest(limit, page) else PaginationRequest(page = page)
        val paginationResponse = postService.listPostsByTopicId(topicId, paginationRequest)
        val header = HttpHeaders()
        header.set("next_page", paginationResponse.nextPage)
        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { buildPostResponse(it) },
            headers = header
        )
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
            HttpJsonResponse.successResponse(buildPostResponse(postService.create(findTopicByIdOrThrow(topicId), userContextService.getCurrentUserDetails(), postDto)))
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

    @RequestMapping(
        path = ["posts/{postId}/likes"],
        method = [RequestMethod.POST]
    )
    @Operation(
        summary = "create a like under the given post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = Like::class))]
        )])
    fun createLike(@PathVariable("postId") postId: String): ResponseEntity<*> = run {
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        val user = userContextService.getCurrentUserDetails()
        if (postService.findLikeByUserIdAndParentId(user.id!!, postId) != null)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already liked this post: ${postId}")
        postService.addLike(postId, user)
        HttpJsonResponse.successResponse(buildPostResponse(post), "User's like added successfully to post: ${postId}")
    }

    @DeleteMapping("posts/{postId}/likes")
    @Operation(
        summary = "delete a like under the given post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = Like::class))]
        )])
    fun deleteLike(@PathVariable("postId") postId: String): ResponseEntity<*> = run {
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        val user = userContextService.getCurrentUserDetails()
        if (postService.findLikeByUserIdAndParentId(user.id!!, postId) == null)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user never liked this post: ${postId}")
        postService.removeLike(postId, user)
        HttpJsonResponse.successResponse(buildPostResponse(post), "User's like deleted successfully to post: ${postId}")
    }

    @RequestMapping(
        path = ["posts/{postId}/views"],
        method = [RequestMethod.POST]
    )
    @Operation(
        summary = "create a view under the given post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = View::class))]
        )])
    fun createView(@PathVariable("postId") postId: String): ResponseEntity<*> = run {
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        val user = userContextService.getCurrentUserDetails()
        if (postService.findViewByUserIdAndParentId(user.id!!, postId) != null)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already viewed this post: ${postId}")
        postService.addView(postId, user)
        HttpJsonResponse.successResponse(buildPostResponse(post), "User's view added successfully to post: ${postId}")
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
            comments = emptyList(), // TODO
            viewcount = viewcount,
            likes = likes,
        )
    }

    private fun buildPostAttachmentResponse(attachment: Attachment): PostResponse.PostAttachment = with(attachment) {
        PostResponse.PostAttachment(
            attachmentId = id,
            fileName = fileName,
            url = savedLocationURL,
            contentType = contentType,
            size = fileSize
        )
    }

    // TODO
    // fun buildPostCommentResponse(comment)
}
