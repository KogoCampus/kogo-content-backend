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
class CommentController @Autowired constructor(
    private val userService: UserService,
    private val commentService: CommentService,
    private val postService: PostService,
    private val notificationService: NotificationService
) {
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
        val author = userService.getCurrentUser()
        val newComment = commentService.create(post, author, commentDto)
        notificationService.createPushNotification(Notification(
            recipientId = post.author.id!!,
            message = NotificationMessage(
                title = "New Comment",
                body =  "There is a new comment in your post",
                data = mapOf(
                    "commentId" to newComment.id!!,
                    "postId" to post.id!!,
                    "commentAuthor" to newComment.author.username,
                    "commentContent" to newComment.content
                ),
            ),
            isPush = true,
            createdAt = newComment.createdAt,
        ))
        HttpJsonResponse.successResponse(CommentResponse.create(commentService.findAggregate(newComment.id!!), author))
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
            headers = [
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
                      ],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = CommentResponse::class)
            )
            )],
        )]
    )
    fun getCommentsOfPost(
        @PathVariable("postId") postId: String,
        paginationRequest: PaginationRequest
    ): ResponseEntity<*> = run {
        val post = findPostByIdOrThrow(postId)
        val paginationResponse = commentService.findAggregatesByPost(post, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map{ CommentResponse.create(it, userService.getCurrentUser()) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

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
        val user = userService.getCurrentUser()

        HttpJsonResponse.successResponse(CommentResponse.create(commentService.findAggregate(comment.id!!), user))
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
        val user = userService.getCurrentUser()

        if (!commentService.isUserAuthor(comment, user)) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the comment"
            )
        }
        val updatedComment = commentService.update(comment, commentUpdate)

        return HttpJsonResponse.successResponse(CommentResponse.create(commentService.findAggregate(updatedComment.id!!), user))
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

        if (!commentService.isUserAuthor(comment, userService.getCurrentUser())) {
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
        val user = userService.getCurrentUser()

        if (commentService.hasUserLikedComment(comment.id!!, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user has already liked this comment Id: $commentId")
        }

        val newLike = commentService.addLike(comment, user)
        notificationService.createPushNotification(Notification(
            recipientId = comment.author.id!!,
            message = NotificationMessage(
                title = "New Like",
                body = "${newLike?.userId} liked your comment",
                data = mapOf(
                    "commentId" to comment.id!!,
                    "userId" to newLike?.userId!!,
                )
            ),
            isPush = true,
            createdAt = newLike.createdAt,
        ))
        return HttpJsonResponse.successResponse(
            CommentResponse.create(commentService.findAggregate(comment.id!!), user),
            "User's like added successfully to comment $commentId"
        )
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
        val user = userService.getCurrentUser()

        if (!commentService.hasUserLikedComment(comment.id!!, user)) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user didn't put a like on this comment Id: $commentId.")
        }

        commentService.removeLike(comment, user)
        return HttpJsonResponse.successResponse(
            CommentResponse.create(commentService.findAggregate(comment.id!!), user),
            "User's like removed successfully to comment $commentId."
        )
    }

    private fun findCommentOrThrow(commentId: String) = run {
        commentService.find(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
    }
    private fun findPostByIdOrThrow(postId: String) = postService.find(postId) ?: throw ResourceNotFoundException.of<Post>(postId)
}
