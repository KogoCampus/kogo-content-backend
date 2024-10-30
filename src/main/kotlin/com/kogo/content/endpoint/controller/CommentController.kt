package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.service.*
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
    private val userContextService: UserContextService,
    private val commentService: CommentService,
    private val replyService: ReplyService
) {
    @GetMapping("comments/{commentId}")
    @Operation(
        summary = "Get a comment from the post or comment",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok - Comment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))],
        )]
    )
    fun getComment(@PathVariable("commentId") commentId: String) = run {
        val comment = findCommentOrThrow(commentId)

        HttpJsonResponse.successResponse(CommentResponse.from(comment))
    }

    @RequestMapping(
        path = ["comments/{commentId}"],
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
    fun updateComment(@PathVariable("commentId") commentId: String, @Valid commentUpdate: CommentUpdate): ResponseEntity<*> {
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
        path = ["comments/{commentId}"],
        method = [RequestMethod.DELETE],
    )
    @Operation(
        summary = "Delete a comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "The comment is deleted.",
        )]
    )
    fun deleteComment(@PathVariable("commentId") commentId: String): ResponseEntity<*> {
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
        path = ["comments/{commentId}/likes"],
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
    fun addLikeToComment(@PathVariable("commentId") commentId: String) : ResponseEntity<*> = run {
        val comment = findCommentOrThrow(commentId)
        val user = userContextService.getCurrentUserDetails()

        if (commentService.hasUserLikedComment(comment, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user has already liked this comment Id: $commentId")
        }

        commentService.addLike(comment, user)
        HttpJsonResponse.successResponse(CommentResponse.from(comment), "User's like added successfully to comment $commentId")
    }

    @DeleteMapping("comments/{commentId}/likes")
    @Operation(
        summary = "Delete a like under the comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Like::class))],
        )]
    )
    fun deleteLikeFromComment(@PathVariable("commentId") commentId: String): ResponseEntity<*> = run {
        val comment = findCommentOrThrow(commentId)
        val user = userContextService.getCurrentUserDetails()

        if (!commentService.hasUserLikedComment(comment, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user didn't put a like on this comment Id: $commentId.")
        }

        commentService.removeLike(comment, user)
        HttpJsonResponse.successResponse(CommentResponse.from(comment), "User's like removed successfully to comment $commentId.")
    }

    @GetMapping("comments/{commentId}/replies")
    @Operation(
        summary = "Get all replies of the comment",
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
            description = "ok - Replies",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = CommentResponse::class)))],
        )]
    )
    fun getRepliesOfComment(@PathVariable("commentId") commentId: String, @RequestParam requestParameters: Map<String, String>): ResponseEntity<*> = run {
        val comment = findCommentOrThrow(commentId)

        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)
        val paginationResponse = replyService.getAllRepliesByComment(comment, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map{ ReplyResponse.from(it) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @RequestMapping(
        path = ["comments/{commentId}/replies"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = CommentDto::class))])
    @Operation(
        summary = "Create a new reply under the comment",
        responses = [ApiResponse(
            responseCode = "201",
            description = "Created a new comment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))],
        )]
    )
    fun createReplyToComment(@PathVariable("commentId") commentId: String, @Valid commentDto: CommentDto) = run {
        val comment = findCommentOrThrow(commentId)

        val author = userContextService.getCurrentUserDetails()
        val newComment = replyService.create(comment, author, commentDto)

        HttpJsonResponse.successResponse(ReplyResponse.from(newComment))
    }

    private fun findCommentOrThrow(commentId: String) = run {
        commentService.find(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
    }
}
