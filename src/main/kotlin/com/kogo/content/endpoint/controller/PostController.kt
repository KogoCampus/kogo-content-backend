package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.service.*
import com.kogo.content.storage.model.DataType
import com.kogo.content.storage.model.EventType
import com.kogo.content.storage.model.NotificationMessage
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
    private val groupService: GroupService,
    private val postService: PostService,
    private val userService: UserService,
    private val notificationService: NotificationService
) {
    @GetMapping("groups/{groupId}/posts")
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
    fun listPostsInGroup(
        @PathVariable("groupId") groupId: String,
        paginationRequest: PaginationRequest
    ): ResponseEntity<*> = run {
        val group = groupService.findOrThrow(groupId)
        val user = userService.findCurrentUser()

        val paginationResponse = postService.findPostsByGroup(group, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.from(it, user) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @RequestMapping(
        path = ["groups/{groupId}/posts"],
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
    fun createPostInGroup(
        @PathVariable("groupId") groupId: String,
        @Valid postDto: PostDto): ResponseEntity<*> {
        val group = groupService.findOrThrow(groupId)
        val user = userService.findCurrentUser()

        // Users cannot create a post if they are not following the group
        if (!group.isFollowing(user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not following group id: ${group.id}")

        val created = postService.create(group, userService.findCurrentUser(), postDto)
        postService.addViewer(created, user)

        return HttpJsonResponse.successResponse(PostResponse.from(created, user))
    }

    @DeleteMapping("groups/{groupId}/posts/{postId}")
    @Operation(
        summary = "delete a post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
        )])
    fun deletePostInGroup(
        @PathVariable("groupId") groupId: String,
        @PathVariable("postId") postId: String
    ): ResponseEntity<*> {
        val group = groupService.findOrThrow(groupId)
        val post = postService.findOrThrow(postId)
        val user = userService.findCurrentUser()

        // Condition: either 1. user is the author of post or 2. user is the owner of group
        if (post.author.id != user.id && group.owner.id != user.id)
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the author of this post or the topic owner")

        postService.delete(post)

        return HttpJsonResponse.successResponse(null)
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
        val user = userService.findCurrentUser()
        val post = postService.findOrThrow(postId)

        postService.addViewer(post, user)

        HttpJsonResponse.successResponse(PostResponse.from(post, user))
    }

    @RequestMapping(
        path = ["groups/{groupId}/posts/{postId}"],
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
        @PathVariable("groupId") groupId: String,
        @PathVariable("postId") postId: String,
        @Valid postUpdate: PostUpdate): ResponseEntity<*> {
        groupService.findOrThrow(groupId)
        val post = postService.find(postId) ?: postService.findOrThrow(postId)
        val user = userService.findCurrentUser()

        // only post author can update the post
        if(post.author.id != user.id)
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the author of this post")

        val updated = postService.update(post, postUpdate)

        return HttpJsonResponse.successResponse(PostResponse.from(updated, user))
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
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun addLikeToPost(@PathVariable("postId") postId: String): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val user = userService.findCurrentUser()

        val wasPreviouslyLiked = post.likes.any { it.userId == user.id }
        val wasLikeAdded = postService.addLikeToPost(post, user)

        if (wasLikeAdded && !wasPreviouslyLiked) {
            // Send notification only if this is the initial like
            notificationService.createPushNotification(
                recipientId = post.author.id!!,
                sender = user,
                eventType = EventType.LIKE_TO_POST,
                message = NotificationMessage(
                    title = post.content.take(50) + if (post.content.length > 50) "..." else "",
                    body = "${user.username} liked your post",
                    dataType = DataType.POST,
                    data = post
                )
            )
        }

        HttpJsonResponse.successResponse(
            data = PostResponse.from(post, user),
            message = if (wasLikeAdded) "User's like added successfully to post: $postId"
                else "User already liked this post: $postId"
        )
    }

    @DeleteMapping("posts/{postId}/likes")
    @Operation(
        summary = "delete a like under the given post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ReplyResponse::class))],
        )])
    fun deleteLikeFromPost(@PathVariable("postId") postId: String): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val user = userService.findCurrentUser()

        val wasRemoved = postService.removeLikeFromPost(post, user)

        HttpJsonResponse.successResponse(
            data = PostResponse.from(post, user),
            message = if (wasRemoved) "User never liked this post: $postId"
                else "User's like deleted successfully to post: $postId")
    }

    @RequestMapping(
        path = ["posts/{postId}/comments/{commentId}/likes"],
        method = [RequestMethod.PUT],
    )
    @Operation(
        summary = "Like a comment",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))],
        )]
    )
    fun addLikeToComment(@PathVariable("postId") postId: String, @PathVariable("commentId") commentId: String): ResponseEntity<*> = run {
        val user = userService.findCurrentUser()
        val post = postService.findOrThrow(postId)
        val comment = postService.findCommentOrThrow(post.id!!, commentId)

        val wasPreviouslyLiked = comment.likes.any { it.userId == user.id }
        val wasLikeAdded = postService.addLikeToComment(post, commentId, user)

        if (wasLikeAdded && !wasPreviouslyLiked) {
            notificationService.createPushNotification(
                recipientId = comment.author.id!!,
                sender = user,
                eventType = EventType.LIKE_TO_COMMENT,
                message = NotificationMessage(
                    title = comment.content.take(50) + if (comment.content.length > 50) "..." else "",
                    body = "${user.username} liked your comment",
                    dataType = DataType.COMMENT,
                    data = comment
                )
            )
        }

        HttpJsonResponse.successResponse(
            CommentResponse.from(comment, user),
            "User's like added successfully to comment $commentId"
        )
    }

    @DeleteMapping("posts/{postId}/comments/{commentId}/likes")
    @Operation(
        summary = "Delete a like under the comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))],
        )]
    )
    fun deleteLikeFromComment(@PathVariable("postId") postId: String, @PathVariable("commentId") commentId: String): ResponseEntity<*> = run {
        val user = userService.findCurrentUser()
        val post = postService.findOrThrow(postId)
        val comment = postService.findCommentOrThrow(post.id!!, commentId)

        if (!comment.likes.any { it.userId == user.id && it.isActive }) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user didn't put a like on this comment Id: $commentId.")
        }

        postService.removeLikeFromComment(post, commentId, user)
        HttpJsonResponse.successResponse(
            CommentResponse.from(comment, user),
            "User's like removed successfully to comment $commentId."
        )
    }

    @RequestMapping(
        path = ["posts/{postId}/comments/{commentId}/replies/{replyId}/likes"],
        method = [RequestMethod.PUT],
    )
    @Operation(
        summary = "Like a reply",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ReplyResponse::class))],
        )]
    )
    fun addLikeToReply(@PathVariable("postId") postId: String, @PathVariable("commentId") commentId: String, @PathVariable("replyId") replyId: String): ResponseEntity<*> = run {
        val user = userService.findCurrentUser()
        val post = postService.findOrThrow(postId)
        val reply = postService.findReplyOrThrow(post.id!!, commentId, replyId)

        val wasPreviouslyLiked = reply.likes.any { it.userId == user.id }
        val wasLikeAdded = postService.addLikeToReply(post, commentId, replyId, user)

        if (wasLikeAdded && !wasPreviouslyLiked) {
            notificationService.createPushNotification(
                recipientId = reply.author.id!!,
                sender = user,
                eventType = EventType.LIKE_TO_REPLY,
                message = NotificationMessage(
                    title = reply.content.take(50) + if (reply.content.length > 50) "..." else "",
                    body = "${user.username} liked your reply",
                    dataType = DataType.REPLY,
                    data = reply
                )
            )
        }

        HttpJsonResponse.successResponse(
            ReplyResponse.from(reply, user),
            "User's like added successfully to reply $replyId"
        )
    }

    @DeleteMapping("posts/{postId}/comments/{commentId}/replies/{replyId}/likes")
    @Operation(
        summary = "Delete a like under the reply",
        responses = [ApiResponse(
            responseCode = "204",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ReplyResponse::class))],
        )]
    )
    fun deleteLikeFromReply(@PathVariable("postId") postId: String, @PathVariable("commentId") commentId: String, @PathVariable("replyId") replyId: String): ResponseEntity<*> = run {
        val user = userService.findCurrentUser()
        val post = postService.findOrThrow(postId)
        val reply = postService.findReplyOrThrow(post.id!!, commentId, replyId)

        if (!reply.likes.any { it.userId == user.id && it.isActive }) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user didn't put a like on this reply Id: $replyId")
        }

        postService.removeLikeFromReply(post, commentId, replyId, user)
        HttpJsonResponse.successResponse(
            ReplyResponse.from(reply, user),
            "User's like removed successfully to reply $replyId"
        )
    }

    @GetMapping("posts/{postId}/comments")
    @Operation(
        summary = "Get all comments from the post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok - All comments",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))]
        )]
    )
    fun getCommentsOfPost(@PathVariable("postId") postId: String): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val user = userService.findCurrentUser()

        val comments = post.comments.map { CommentResponse.from(it, user) }
        HttpJsonResponse.successResponse(comments)
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
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))]
        )]
    )
    fun createCommentToPost(
        @PathVariable("postId") postId: String,
        @Valid commentDto: CommentDto
    ): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val author = userService.findCurrentUser()

        val newComment = postService.addCommentToPost(post, commentDto.content, author)
        notificationService.createPushNotification(
            recipientId = post.author.id!!,
            eventType = EventType.CREATE_COMMENT_TO_POST,
            sender = author,
            message = NotificationMessage(
                title = commentDto.content.take(50) + if (commentDto.content.length > 50) "..." else "",
                body = "${author.username} commented on your post",
                dataType = DataType.COMMENT,
                data = newComment
            )
        )

        HttpJsonResponse.successResponse(CommentResponse.from(newComment, author))
    }

    @RequestMapping(
        path = ["posts/{postId}/comments/{commentId}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = CommentUpdate::class))])
    @Operation(
        summary = "Update a comment",
        responses = [ApiResponse(
            responseCode = "200",
            description = "Updated comment",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentResponse::class))]
        )]
    )
    fun updateComment(
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @Valid commentUpdate: CommentUpdate
    ): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val comment = postService.findCommentOrThrow(post.id!!, commentId)
        val user = userService.findCurrentUser()

        if (comment.author.id != user.id) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the comment"
            )
        }

        val updatedComment = postService.updateComment(post, commentId, commentUpdate)
        HttpJsonResponse.successResponse(CommentResponse.from(updatedComment, user))
    }

    @RequestMapping(
        path = ["posts/{postId}/comments/{commentId}"],
        method = [RequestMethod.DELETE],
    )
    @Operation(
        summary = "Delete a comment",
        responses = [ApiResponse(
            responseCode = "204",
            description = "The comment is deleted."
        )]
    )
    fun deleteComment(@PathVariable("postId") postId: String, @PathVariable("commentId") commentId: String): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val user = userService.findCurrentUser()
        val comment = postService.findCommentOrThrow(post.id!!, commentId)

        if (comment.author.id != user.id) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the comment"
            )
        }
        postService.removeCommentFromPost(post, commentId)

        HttpJsonResponse.successResponse(null)
    }

    @GetMapping("posts/{postId}/comments/{commentId}/replies")
    @Operation(
        summary = "Get all replies from the comment",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok - All replies",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ReplyResponse::class))]
        )]
    )
    fun getRepliesOfComment(
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String
    ): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val comment = postService.findCommentOrThrow(post.id!!, commentId)
        val user = userService.findCurrentUser()

        val replies = comment.replies.map { ReplyResponse.from(it, user) }
        HttpJsonResponse.successResponse(replies)
    }

    @RequestMapping(
        path = ["posts/{postId}/comments/{commentId}/replies"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = CommentDto::class))])
    @Operation(
        summary = "Create a new reply under the comment",
        responses = [ApiResponse(
            responseCode = "201",
            description = "Created a new reply",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ReplyResponse::class))]
        )]
    )
    fun createReplyToComment(
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @Valid commentDto: CommentDto
    ): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val comment = postService.findCommentOrThrow(post.id!!, commentId)
        val author = userService.findCurrentUser()

        val newReply = postService.addReplyToComment(post, commentId, commentDto.content, author)

        notificationService.createPushNotification(
            recipientId = comment.author.id!!,
            sender = author,
            eventType = EventType.CREATE_REPLY_TO_COMMENT,
            message = NotificationMessage(
                title = commentDto.content.take(50) + if (commentDto.content.length > 50) "..." else "",
                body = "${author.username} replied to your comment",
                dataType = DataType.REPLY,
                data = newReply
            )
        )

        HttpJsonResponse.successResponse(ReplyResponse.from(newReply, author))
    }

    @RequestMapping(
        path = ["posts/{postId}/comments/{commentId}/replies/{replyId}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = CommentUpdate::class))])
    @Operation(
        summary = "Update a reply",
        responses = [ApiResponse(
            responseCode = "200",
            description = "Updated reply",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ReplyResponse::class))]
        )]
    )
    fun updateReply(
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @PathVariable("replyId") replyId: String,
        @Valid commentUpdate: CommentUpdate
    ): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val reply = postService.findReplyOrThrow(post.id!!, commentId, replyId)
        val user = userService.findCurrentUser()

        if (reply.author.id != user.id) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the reply"
            )
        }

        val updatedReply = postService.updateReply(post, commentId, replyId, commentUpdate)
        HttpJsonResponse.successResponse(ReplyResponse.from(updatedReply, user))
    }

    @DeleteMapping("posts/{postId}/comments/{commentId}/replies/{replyId}")
    @Operation(
        summary = "Delete a reply",
        responses = [ApiResponse(
            responseCode = "204",
            description = "The reply is deleted."
        )]
    )
    fun deleteReply(
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
        @PathVariable("replyId") replyId: String
    ): ResponseEntity<*> = run {
        val post = postService.findOrThrow(postId)
        val reply = postService.findReplyOrThrow(post.id!!, commentId, replyId)
        val user = userService.findCurrentUser()

        if (reply.author.id != user.id) {
            return HttpJsonResponse.errorResponse(
                errorCode = ErrorCode.USER_ACTION_DENIED,
                "user is not the author of the reply"
            )
        }

        postService.removeReplyFromComment(post, commentId, replyId)
        HttpJsonResponse.successResponse(null)
    }
}
