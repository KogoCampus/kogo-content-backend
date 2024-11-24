package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.service.*
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
    private val commentService: CommentService,
    private val userService: UserService,
    private val topicService: TopicService,
    private val notificationService: NotificationService
) {
    @GetMapping("topics/{topicId}/posts")
    @Operation(
        summary = "return a list of posts in the given topic",
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
                schema = Schema(implementation = PostResponse::class))
            )],
        )])
    fun listPostsInTopic(
        @PathVariable("topicId") topicId: String,
        paginationRequest: PaginationRequest
    ): ResponseEntity<*> = run {
        val topic = findTopicByIdOrThrow(topicId)
        val user = userService.getCurrentUser()
        val paginationResponse = postService.findPostsByTopic(topic, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.create(it, user) },
            headers = paginationResponse.toHttpHeaders()
        )
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
    fun createPostToTopic(
        @PathVariable("topicId") topicId: String,
        @Valid postDto: PostDto): ResponseEntity<*> {
        val topic = findTopicByIdOrThrow(topicId)
        val user = userService.getCurrentUser()

        if (!topicService.hasUserFollowedTopic(topic, user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not following topic id: ${topic.id!!}")

        val post = postService.create(topic, userService.getCurrentUser(), postDto)
        postService.markPostViewedByUser(post.id!!, user.id!!)
        return HttpJsonResponse.successResponse(PostResponse.create(postService.findAggregate(post.id!!), user))
    }

    @DeleteMapping("topics/{topicId}/posts/{postId}")
    @Operation(
        summary = "delete a post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
        )])
    fun deletePostInTopic(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String
    ): ResponseEntity<*> {
        val topic = findTopicByIdOrThrow(topicId)
        val post = findPostByIdOrThrow(postId)
        val user = userService.getCurrentUser()

        if(!postService.isPostAuthor(post, user) && !topicService.isUserTopicOwner(topic, user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the author of this post or the topic owner")

        val deletedPost = postService.delete(post)
        return HttpJsonResponse.successResponse(deletedPost)
    }

    @GetMapping("posts/{postId}")
    @Operation(
        summary = "return a post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun getPost(@PathVariable("postId") postId: String) = run {
        val user = userService.getCurrentUser()
        val post = findPostByIdOrThrow(postId)
        postService.markPostViewedByUser(post.id!!, user.id!!)

        HttpJsonResponse.successResponse(PostResponse.create(postService.findAggregate(post.id!!), user))
    }

    @RequestMapping(
        path = ["posts/{postId}"],
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
    fun updatePost(@PathVariable("postId") postId: String, @Valid postUpdate: PostUpdate): ResponseEntity<*> {
        val post = postService.find(postId) ?: findPostByIdOrThrow(postId)
        val user = userService.getCurrentUser()

        if(!postService.isPostAuthor(post, user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the author of this post")

        val updatedPost = postService.update(post, postUpdate)
        return HttpJsonResponse.successResponse(PostResponse.create(postService.findAggregate(updatedPost.id!!), user))
    }

    @RequestMapping(
        path = ["posts/{postId}/likes"],
        method = [RequestMethod.PUT]
    )
    @Operation(
        summary = "create a like under the given post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = Like::class))]
        )])
    fun addLike(@PathVariable("postId") postId: String): ResponseEntity<*> = run {
        val post = postService.find(postId) ?: findPostByIdOrThrow(postId)
        val user = userService.getCurrentUser()

        if (postService.hasUserLikedPost(post, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already liked this post: $postId")

        val newLike = postService.addLike(post, user)
        notificationService.createPushNotification(
            recipientId = post.author.id!!,
            message = NotificationMessage(
                title = "New Like",
                body = "${user.id} liked your post",
                data = mapOf(
                    "postId" to post.id!!,
                    "userId" to user.id!!,
                )
            )
        )
        HttpJsonResponse.successResponse(PostResponse.create(postService.findAggregate(post.id!!), user), "User's like added successfully to post: $postId")
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
        val post = postService.find(postId) ?: findPostByIdOrThrow(postId)
        val user = userService.getCurrentUser()

        if (!postService.hasUserLikedPost(post, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user never liked this post: $postId")

        postService.removeLike(post, user)
        HttpJsonResponse.successResponse(PostResponse.create(postService.findAggregate(post.id!!), user), "User's like deleted successfully to post: $postId")
    }

    private fun findTopicByIdOrThrow(topicId: String) = topicService.find(topicId) ?: throw ResourceNotFoundException.of<Topic>(topicId)
    private fun findPostByIdOrThrow(postId: String) = postService.find(postId) ?: throw ResourceNotFoundException.of<Post>(postId)
}
