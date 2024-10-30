package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.service.CommentService
import com.kogo.content.service.UserContextService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.storage.entity.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
    private val topicService: TopicService,
    private val postService: PostService,
    private val commentService: CommentService,
    private val userContextService: UserContextService
) {
    @GetMapping("posts/{postId}")
    @Operation(
        summary = "return a post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun getPost(@PathVariable("postId") postId: String) = run {
        val user = userContextService.getCurrentUserDetails()
        val post = postService.find(postId) ?: findPostByIdOrThrow(postId)
        postService.addView(post, user)
        HttpJsonResponse.successResponse(PostResponse.from(post, createPostUserActivityResponse(post)))
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
        val user = userContextService.getCurrentUserDetails()

        if(!postService.isPostAuthor(post, user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the author of this post")

        val updatedPost = postService.update(post, postUpdate)
        return HttpJsonResponse.successResponse(PostResponse.from(updatedPost, createPostUserActivityResponse(updatedPost)))
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
        val user = userContextService.getCurrentUserDetails()

        if (postService.hasUserLikedPost(post, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already liked this post: $postId")

        postService.addLike(post, user)
        HttpJsonResponse.successResponse(PostResponse.from(post, createPostUserActivityResponse(post)), "User's like added successfully to post: $postId")
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
        val user = userContextService.getCurrentUserDetails()

        if (!postService.hasUserLikedPost(post, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user never liked this post: $postId")

        postService.removeLike(post, user)
        HttpJsonResponse.successResponse(PostResponse.from(post, createPostUserActivityResponse(post)), "User's like deleted successfully to post: $postId")
    }

    @RequestMapping(
        path = ["posts/{postId}/comments"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = CommentDto::class))])
    @Operation(
        summary = "Create a new comment",
        responses = [ApiResponse(
            responseCode = "201",
            description = "Created a new comment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))],
        )]
    )
    fun createCommentToPost(@PathVariable("postId") postId: String, @Valid commentDto: CommentDto) = run {
        val post = findPostByIdOrThrow(postId)

        val author = userContextService.getCurrentUserDetails()
        val newComment = commentService.create(post, author, commentDto)

        HttpJsonResponse.successResponse(CommentResponse.from(newComment))
    }

    @GetMapping("posts/{postId}/comments")
    @Operation(
        summary = "Get all comments from the post",
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
            description = "ok - All comments",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = CommentResponse::class)
            ))],
        )]
    )
    fun getCommentsOfPost(@PathVariable("postId") postId: String, @RequestParam requestParameters: Map<String, String>): ResponseEntity<*> = run {
        val post = findPostByIdOrThrow(postId)

        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)
        val paginationResponse = commentService.listCommentsByPost(post, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map{ CommentResponse.from(it) },
            headers = paginationResponse.toHttpHeaders()
        )
    }


    private fun findPostByIdOrThrow(postId: String): Post = postService.find(postId) ?: throw ResourceNotFoundException.of<Post>(postId)

    private fun createPostUserActivityResponse(post: Post): PostResponse.PostUserActivity {
        val like = postService.findLike(post, userContextService.getCurrentUserDetails())
        val view = postService.findView(post, userContextService.getCurrentUserDetails())
        return PostResponse.PostUserActivity(
            liked = like != null,
            likedAt = like?.createdAt,
            viewed = view != null,
            viewedAt = view?.createdAt,
        )
    }
}
