package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.service.CommentService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserContextService
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.storage.entity.*
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("media")
class CommentController @Autowired constructor(
    private val commentService: CommentService,
    private val postService: PostService,
    private val userContextService: UserContextService,
    private val topicService: TopicService,
) {
    @GetMapping("topics/{topicId}/posts/{postId}/comments")
    @Operation(
        summary = "Get all comments from the post",
        parameters = [Parameter(schema = Schema(implementation = PaginationRequest::class))],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok - All comments",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = CommentResponse::class)
            ))],
        )]
    )
    fun getComments(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @RequestParam requestParameters: Map<String, String>
    ): ResponseEntity<*> = run {
        val post = findPostOrThrow(postId)

        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)
        val paginationResponse = commentService.listCommentsByPost(post, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map{ CommentResponse.from(it) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @GetMapping("topics/{topicId}/posts/{postId}/comments/{commentId}")
    @Operation(
        summary = "Get a comment from the post or comment",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok - Comment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))],
        )]
    )
    fun getComment(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
    ) = run {
        val comment = findCommentOrThrow(commentId)

        HttpJsonResponse.successResponse(CommentResponse.from(comment))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments"],
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
    fun createComment(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @Valid commentDto: CommentDto,
    ) = run {
        val post = findPostOrThrow(postId)

        val author = userContextService.getCurrentUserDetails()
        val newComment = commentService.create(post, author, commentDto)

        HttpJsonResponse.successResponse(CommentResponse.from(newComment))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}"],
        method = [RequestMethod.PUT],
    )
    @Operation(
        summary = "Update a comment",
        responses = [ApiResponse(
            responseCode = "200",
            description = "Updated comment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))],
        )]
    )
    @RequestBody(content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentUpdate::class))])
    fun updateComment(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @Valid commentUpdate: CommentUpdate,
    ): ResponseEntity<*> {
        val comment = findCommentOrThrow(commentId)

        if (!commentService.isUserAuthor(comment, userContextService.getCurrentUserDetails())) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the comment"
            )
        }
        val updatedComment = commentService.update(comment, commentUpdate)

        return HttpJsonResponse.successResponse(CommentResponse.from(updatedComment))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}"],
        method = [RequestMethod.DELETE],
    )
    @Operation(
        summary = "Delete a comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "The comment is deleted.",
        )]
    )
    fun deleteComment(
        @PathVariable("topicId") topicId: String,
        @PathVariable("commentId") commentId: String,
        @PathVariable("postId") postId: String,
    ): ResponseEntity<*> {
        val comment = findCommentOrThrow(commentId)

        if (!commentService.isUserAuthor(comment, userContextService.getCurrentUserDetails())) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the comment"
            )
        }
        val deletedComment = commentService.delete(comment)

        return HttpJsonResponse.successResponse(deletedComment)
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}/likes"],
        method = [RequestMethod.PUT],
    )
    @Operation(
        summary = "Like a comment",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = Like::class))]
        )]
    )
    fun addLikeToComment(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
    ) : ResponseEntity<*> = run {
        val comment = findCommentOrThrow(commentId)
        val user = userContextService.getCurrentUserDetails()

        if (commentService.hasUserLikedComment(comment, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user has already liked this comment Id: $commentId")
        }

        commentService.addLike(comment, user)
        HttpJsonResponse.successResponse(CommentResponse.from(comment), "User's like added successfully to comment $commentId")
    }

    @DeleteMapping("topics/{topicId}/posts/{postId}/comments/{commentId}/likes")
    @Operation(
        summary = "Delete a like under the comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Like::class))],
        )]
    )
    fun deleteLikeFromComment(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
    ): ResponseEntity<*> = run {
        val comment = findCommentOrThrow(commentId)
        val user = userContextService.getCurrentUserDetails()

        if (!commentService.hasUserLikedComment(comment, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user didn't put a like on this comment Id: $commentId.")
        }

        commentService.removeLike(comment, user)
        HttpJsonResponse.successResponse(CommentResponse.from(comment), "User's like removed successfully to comment $commentId.")
    }

    private fun findTopicOrThrow(topicId: String) = run {
        topicService.find(topicId) ?: throw ResourceNotFoundException(resourceName = "Topic", resourceId = topicId)
    }

    private fun findPostOrThrow(postId: String) = run {
        postService.find(postId) ?: throw ResourceNotFoundException(resourceName = "Post", resourceId = postId)
    }

    private fun findCommentOrThrow(commentId: String) = run {
        commentService.find(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
    }
}
