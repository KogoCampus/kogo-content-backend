package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.service.entity.CommentService
import com.kogo.content.service.entity.PostService
import com.kogo.content.service.entity.TopicService
import com.kogo.content.service.entity.UserContextService
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
class ReplyController @Autowired constructor(
    private val commentService: CommentService,
    private val postService: PostService,
    private val userContextService: UserContextService,
    private val topicService: TopicService,
) {
    @GetMapping("topics/{topicId}/posts/{postId}/comments/{commentId}/replies")
    @Operation(
        summary = "Get all replies of the comment",
        parameters = [Parameter(schema = Schema(implementation = PaginationRequest::class))],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok - Replies",
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = CommentResponse::class)))],
        )]
    )
    fun getReplies(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @RequestParam requestParameters: Map<String, String>
    ): ResponseEntity<*> = run {
        val topic = findTopicOrThrow(topicId)
        val post = findPostOrThrow(postId)
        val comment = findCommentOrThrow(commentId)

        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)
        val paginationResponse = commentService.listRepliesByComment(comment, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map{ ReplyResponse.from(it) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}/replies"],
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
    fun createReply(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @Valid commentDto: CommentDto,
    ) = run {
        val topic = findTopicOrThrow(topicId)
        val post = findPostOrThrow(postId)
        val comment = findCommentOrThrow(commentId)

        val author = userContextService.getCurrentUserDetails()
        val newComment = commentService.createReply(comment, author, commentDto)

        HttpJsonResponse.successResponse(ReplyResponse.from(newComment))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}/replies/{replyId}"],
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
    fun updateReply(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @PathVariable("replyId") replyId: String,
        @Valid commentUpdate: CommentUpdate,
    ): ResponseEntity<*> {
        val topic = findTopicOrThrow(topicId)
        val post = findPostOrThrow(postId)
        val comment = findCommentOrThrow(commentId)
        val reply = findReplyOrThrow(replyId)

        if (!commentService.isReplyAuthor(reply, userContextService.getCurrentUserDetails())) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the reply"
            )
        }
        val updatedReply = commentService.updateReply(reply, commentUpdate)

        return HttpJsonResponse.successResponse(ReplyResponse.from(updatedReply))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}/replies/{replyId}"],
        method = [RequestMethod.DELETE],
    )
    @Operation(
        summary = "Delete a comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "The comment is deleted.",
        )]
    )
    fun deleteReply(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @PathVariable("replyId") replyId: String,
    ): ResponseEntity<*> {
        val topic = findTopicOrThrow(topicId)
        val post = findPostOrThrow(postId)
        val comment = findCommentOrThrow(commentId)
        val reply = findReplyOrThrow(replyId)

        if (!commentService.isReplyAuthor(reply, userContextService.getCurrentUserDetails())) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the reply"
            )
        }
        val deletedReply = commentService.deleteReply(reply)

        return HttpJsonResponse.successResponse(deletedReply)
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}/replies/{replyId}/likes"],
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
    fun addLikeToReply(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @PathVariable("replyId") replyId: String,
    ) : ResponseEntity<*> = run {
        val topic = findTopicOrThrow(topicId)
        val post = findPostOrThrow(postId)
        val comment = findCommentOrThrow(commentId)
        val reply = findReplyOrThrow(replyId)
        val user = userContextService.getCurrentUserDetails()

        if (commentService.hasUserLikedReply(reply, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already liked this reply $replyId.")
        }

        commentService.addLikeToReply(reply, user)
        HttpJsonResponse.successResponse(ReplyResponse.from(reply), "User's like added successfully to comment $replyId.")
    }

    @DeleteMapping("topics/{topicId}/posts/{postId}/comments/{commentId}/replies/{replyId}/likes")
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
        @PathVariable("replyId") replyId: String,
    ): ResponseEntity<*> = run {
        val topic = findTopicOrThrow(topicId)
        val post = findPostOrThrow(postId)
        val comment = findCommentOrThrow(commentId)
        val reply = findReplyOrThrow(replyId)
        val user = userContextService.getCurrentUserDetails()

        if (commentService.hasUserLikedReply(reply, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already liked this reply $replyId.")
        }

        commentService.removeLikeFromReply(reply, user)
        HttpJsonResponse.successResponse(ReplyResponse.from(reply), "User's like removed successfully to reply $replyId.")
    }

    private fun findTopicOrThrow(topicId: String) = run {
        topicService.find(topicId) ?: throw ResourceNotFoundException(resourceName = "Topic", resourceId = topicId)
    }

    private fun findPostOrThrow(postId: String) = run {
        postService.find(postId) ?: throw ResourceNotFoundException(resourceName = "Post", resourceId = postId)
    }

    private fun findCommentOrThrow(commentId: String) = run {
        commentService.findComment(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
    }

    private fun findReplyOrThrow(replyId: String) = run {
        commentService.findReply(replyId) ?: throw ResourceNotFoundException("Reply", replyId)
    }
}
