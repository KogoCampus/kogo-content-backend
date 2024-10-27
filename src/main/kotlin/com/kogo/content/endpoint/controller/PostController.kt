package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.endpoint.model.AttachmentResponse
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.service.entity.UserContextService
import com.kogo.content.service.entity.PostService
import com.kogo.content.service.entity.TopicService
import com.kogo.content.service.pagination.PaginationRequest
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
@RequestMapping("media")
class PostController @Autowired constructor(
    private val postService: PostService,
    private val topicService: TopicService,
    private val userContextService: UserContextService
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
        @PathVariable("topicId") topicId: String, @RequestParam requestParameters: Map<String, String>): ResponseEntity<*> = run {
        val topic = findTopicByIdOrThrow(topicId)
        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)

        val paginationResponse = postService.listPostsByTopicId(topicId, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.from(it) },
            headers = paginationResponse.toHttpHeaders()
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
        val topic = findTopicByIdOrThrow(topicId)
        val user = userContextService.getCurrentUserDetails()
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        postService.addView(post, user)
        HttpJsonResponse.successResponse(PostResponse.from(post))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = PostDto::class))])
    @Operation(
        summary = "create a post under the given topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun createPost(
        @PathVariable("topicId") topicId: String,
        @Valid postDto: PostDto): ResponseEntity<*> {
            val topic = findTopicByIdOrThrow(topicId)
            val user = userContextService.getCurrentUserDetails()

            if (!topicService.isUserFollowingTopic(topic, user))
                return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "Failed to create a post: user is not following ${topic.topicName}")

            val post = postService.create(topic, userContextService.getCurrentUserDetails(), postDto)
            return HttpJsonResponse.successResponse(PostResponse.from(post))
        }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = PostUpdate::class))])
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
        @Valid postUpdate: PostUpdate): ResponseEntity<*> {
        val topic = findTopicByIdOrThrow(topicId)
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        val user = userContextService.getCurrentUserDetails()

        if(!postService.isPostAuthor(post, user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the author of this post")

        val updatedPost = postService.update(post, postUpdate)
        return HttpJsonResponse.successResponse(PostResponse.from(post))
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
        @PathVariable("postId") postId: String
    ): ResponseEntity<*> {
        val topic = findTopicByIdOrThrow(topicId)
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        val user = userContextService.getCurrentUserDetails()

        if(!postService.isPostAuthor(post, user) && !topicService.isTopicOwner(topic, user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the author of this post or the topic owner")

        val deletedPost = postService.delete(post)
        return HttpJsonResponse.successResponse(deletedPost)
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/likes"],
        method = [RequestMethod.PUT]
    )
    @Operation(
        summary = "create a like under the given post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = Like::class))]
        )])
    fun addLike(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String
    ): ResponseEntity<*> = run {
        val topic = findTopicByIdOrThrow(topicId)
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        val user = userContextService.getCurrentUserDetails()

        if (postService.hasUserLikedPost(post, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already liked this post: $postId")

        postService.addLike(post, user)
        HttpJsonResponse.successResponse(PostResponse.from(post), "User's like added successfully to post: $postId")
    }

    @DeleteMapping("topics/{topicId}/posts/{postId}/likes")
    @Operation(
        summary = "delete a like under the given post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = Like::class))]
        )])
    fun deleteLike(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String
    ): ResponseEntity<*> = run {
        val topic = findTopicByIdOrThrow(topicId)
        val post = postService.find(postId) ?: throwPostNotFound(postId)
        val user = userContextService.getCurrentUserDetails()

        if (postService.hasUserLikedPost(post, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user never liked this post: $postId")

        postService.removeLike(post, user)
        HttpJsonResponse.successResponse(PostResponse.from(post), "User's like deleted successfully to post: $postId")
    }

    @GetMapping("topics/{topicId}/posts/search")
    @Operation(
        summary = "search posts containing the keyword",
        parameters = [Parameter(schema = Schema(implementation = PaginationRequest::class))],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [Header(name = "next_page", schema = Schema(type = "string"))],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)))],
        )])
    fun searchPosts(
        @RequestParam("q") keyword: String,
        @RequestParam("limit") limit: Int?,
        @RequestParam("pageToken") page: String?): Nothing = run {
        throwPostNotFound("")
        TODO("" +
            "1. remove return type Nothing" +
            "2. use(create) a function inside postService to get Post Pagination using the keyword" +
            "3. return the successResponse with the pagination result"
        )

//        val paginationResponse = postService.listPostsByKeyword(keyword, paginationRequest)
//        HttpJsonResponse.successResponse(
//            data = paginationResponse.items.map { buildPostResponse(it, it.topic) },
//            headers = paginationResponse.toHeaders()
//        )

    }

    private fun findTopicByIdOrThrow(topicId: String): Topic = topicService.find(topicId) ?: throw ResourceNotFoundException.of<Topic>(topicId)

    private fun throwPostNotFound(postId: String): Nothing = throw ResourceNotFoundException.of<Post>(postId)
}
