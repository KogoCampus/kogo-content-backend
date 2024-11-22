package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.service.*
import com.kogo.content.storage.entity.*
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@RestController
@RequestMapping("media")
class ReplyController @Autowired constructor(
    private val commentService: CommentService,
    private val replyService: ReplyService,
    private val userService: UserService,
    private val notificationService: NotificationService,
) {
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
                name = PaginationRequest.SORT_PARAM,
                description = "sort fields (format: field1:asc,field2:desc)",
                schema = Schema(type = "string"),
                required = false
            ),
            Parameter(
                name = PaginationRequest.FILTER_PARAM,
                description = "filter fields (format: field1:value1,field2:value2)",
                schema = Schema(type = "string"),
                required = false
            ),
            Parameter(
                name = PaginationRequest.PAGE_SIZE_PARAM,
                description = "limit for pagination",
                schema = Schema(type = "integer", defaultValue = "10"),
                required = false
            )
        ],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok - Replies",
            headers = [
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
                Header(name = PaginationSlice.HEADER_PAGE_SIZE, schema = Schema(type = "string")),
            ],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = CommentResponse::class))
            )],
        )]
    )
    fun getRepliesOfComment(
        @PathVariable("commentId") commentId: String,
        paginationRequest: PaginationRequest
    ): ResponseEntity<*> = run {
        val comment = findCommentOrThrow(commentId)
        val user = userService.getCurrentUser()
        val paginationResponse = replyService.findReplyAggregatesByComment(comment, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { ReplyResponse.create(it, user) },
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
        val author = userService.getCurrentUser()
        val newReply = replyService.create(comment, author, commentDto)
        notificationService.createPushNotification(
            recipientId = comment.author.id!!,
            message = NotificationMessage(
                title = "New Reply",
                body =  "There is a new reply in your comment",
                data = mapOf(
                    "replyId" to newReply.id!!,
                    "commentId" to comment.id!!,
                    "replyAuthor" to newReply.author.username,
                    "replyContent" to newReply.content
                ),
            )
        )

        HttpJsonResponse.successResponse(ReplyResponse.create(replyService.findAggregate(newReply.id!!), author))
    }

    @RequestMapping(
        path = ["replies/{replyId}"],
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
    fun updateReply(@PathVariable("replyId") replyId: String, @Valid commentUpdate: CommentUpdate): ResponseEntity<*> {
        val reply = findReplyOrThrow(replyId)
        val user = userService.getCurrentUser()

        if (!replyService.isUserAuthor(reply, user)) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the reply"
            )
        }
        val updatedReply = replyService.update(reply, commentUpdate)

        return HttpJsonResponse.successResponse(ReplyResponse.create(replyService.findAggregate(updatedReply.id!!), user))
    }

    @RequestMapping(
        path = ["replies/{replyId}"],
        method = [RequestMethod.DELETE],
    )
    @Operation(
        summary = "Delete a comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "The comment is deleted.",
        )]
    )
    fun deleteReply(@PathVariable("replyId") replyId: String): ResponseEntity<*> {
        val reply = findReplyOrThrow(replyId)

        if (!replyService.isUserAuthor(reply, userService.getCurrentUser())) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the reply"
            )
        }
        val deletedReply = replyService.delete(reply)

        return HttpJsonResponse.successResponse(deletedReply)
    }

    @RequestMapping(
        path = ["replies/{replyId}/likes"],
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
    fun addLikeToReply(@PathVariable("replyId") replyId: String) : ResponseEntity<*> = run {
        val reply = findReplyOrThrow(replyId)
        val user = userService.getCurrentUser()

        if (replyService.hasUserLikedReply(reply, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user has already liked this reply Id: $replyId")
        }

        val newLike = replyService.addLike(reply, user)
        notificationService.createPushNotification(
            recipientId = reply.author.id!!,
            message = NotificationMessage(
                title = "New Like",
                body = "${user.id} liked your reply",
                data = mapOf(
                    "replyId" to reply.id!!,
                    "userId" to user.id!!,
                )
            )
        )
        HttpJsonResponse.successResponse(
           ReplyResponse.create(replyService.findAggregate(reply.id!!), user),
            "User's like added successfully to reply $replyId"
        )
    }

    @DeleteMapping("replies/{replyId}/likes")
    @Operation(
        summary = "Delete a like under the comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Like::class))],
        )]
    )
    fun deleteLikeFromReply(@PathVariable("replyId") replyId: String): ResponseEntity<*> = run {
        val reply = findReplyOrThrow(replyId)
        val user = userService.getCurrentUser()

        if (!replyService.hasUserLikedReply(reply, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user didn't put a like on this reply Id: $replyId")
        }

        replyService.removeLike(reply, user)
        HttpJsonResponse.successResponse(
            ReplyResponse.create(replyService.findAggregate(reply.id!!), user),
            "User's like removed successfully to reply $replyId"
        )
    }

    private fun findCommentOrThrow(commentId: String) = run {
        commentService.find(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
    }

    private fun findReplyOrThrow(replyId: String) = run {
        replyService.find(replyId) ?: throw ResourceNotFoundException("Reply", replyId)
    }
}
