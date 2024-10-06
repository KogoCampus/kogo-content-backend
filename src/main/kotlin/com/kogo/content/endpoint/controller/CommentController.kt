package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.CommentDto
import com.kogo.content.endpoint.model.CommentResponse
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.searchengine.Document
import com.kogo.content.searchengine.SearchIndex
import com.kogo.content.searchengine.SearchIndexService
import com.kogo.content.service.CommentService
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.CommentRepository
import com.kogo.content.storage.repository.PostRepository
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import java.time.Instant
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("media")
class CommentController @Autowired constructor(
    private val commentService: CommentService,
    private val postService: PostService,
    private val userContextService: UserContextService,
    private val searchIndexService: SearchIndexService,
    private val topicService: TopicService,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
) {
    @GetMapping("topics/{topicId}/posts/{postId}/comments")
    @Operation(
        summary = "Get all comments from the post",
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
        @PathVariable("postId") postId: String
    ) = run {
        findTopicAndPost(topicId, postId)
        HttpJsonResponse.successResponse(commentService.findCommentsByParentId(postId).map{ buildCommentResponse(it) })
    }

    @GetMapping("topics/{topicId}/posts/{postId}/comments/{commentId}/replies")
    @Operation(
        summary = "Get all replies of the comment",
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
    ) = run {
        findTopicAndPost(topicId, postId)
        findComment(commentId)
        val replies = commentService.findCommentsByParentId(commentId)
        HttpJsonResponse.successResponse(replies.map{ buildCommentResponse(it) })
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
        findTopicAndPost(topicId, postId)
        val comment = commentService.find(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
        HttpJsonResponse.successResponse(buildCommentResponse(comment))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments"],
        method = [RequestMethod.POST],
    )
    @RequestBody(content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentDto::class))])
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
        findTopicAndPost(topicId, postId)
        val author = userContextService.getCurrentUserDetails()
        val newComment = commentService.create(postId, CommentParentType.POST, author, commentDto)
        val commentDocument = buildCommentIndexDocument(newComment)
        searchIndexService.addDocument(SearchIndex.COMMENTS, commentDocument)
        HttpJsonResponse.successResponse(buildCommentResponse(newComment))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}/replies"],
        method = [RequestMethod.POST],
    )
    @RequestBody(content = [Content(mediaType = "application/json", schema = Schema(implementation = CommentDto::class))])
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
        findTopicAndPost(topicId, postId)
        findComment(commentId)
        val author = userContextService.getCurrentUserDetails()
        val newComment = commentService.create(commentId, CommentParentType.COMMENT, author, commentDto)
        val commentDocument = buildCommentIndexDocument(newComment)
        searchIndexService.addDocument(SearchIndex.COMMENTS, commentDocument)
        HttpJsonResponse.successResponse(buildCommentResponse(newComment))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}"],
        method = [RequestMethod.DELETE],
    )
    @Operation(
        summary = "Delete a comment/reply",
        responses = [ApiResponse(
            responseCode = "204",
            description = "The comment is deleted.",
        )]
    )
    fun deleteComment(
        @PathVariable("topicId") topicId: String,
        @PathVariable("commentId") commentId: String,
        @PathVariable("postId") postId: String,
    ) = run {
        findTopicAndPost(topicId, postId)
        findComment(commentId)
        val deletedComment = commentService.delete(commentId)
        searchIndexService.deleteDocument(SearchIndex.COMMENTS, commentId)
        HttpJsonResponse.successResponse(deletedComment)
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}"],
        method = [RequestMethod.PUT],
    )
    @Operation(
        summary = "Update a comment/reply",
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
    ) = run {
        // check comment exist
        findTopicAndPost(topicId, postId)
        val comment = findComment(commentId)

        val newComment = commentService.update(comment, commentUpdate)
        val updateCommentDocument = buildCommentIndexDocumentUpdate(commentId, commentUpdate)
        searchIndexService.updateDocument(SearchIndex.COMMENTS, updateCommentDocument)
        HttpJsonResponse.successResponse(buildCommentResponse(newComment))
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts/{postId}/comments/{commentId}/likes"],
        method = [RequestMethod.POST],
    )
    @Operation(
        summary = "Like a comment",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = Like::class))]
        )]
    )
    fun createLike(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
    ) : ResponseEntity<*> = run {
        findTopicAndPost(topicId, postId)
        findComment(commentId)
        val user = userContextService.getCurrentUserDetails()
        if (commentService.findLikeByUserIdAndParentId(user.id!!, commentId) != null) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already liked this comment $commentId.")
        }
        commentService.addLike(commentId, user)
        HttpJsonResponse.successResponse(buildCommentResponse(findComment(commentId)), "User's like added successfully to comment $commentId.")
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
    fun deleteLike(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String,
        @PathVariable("commentId") commentId: String,
    ): ResponseEntity<*> = run {
        findTopicAndPost(topicId, postId)
        val user = userContextService.getCurrentUserDetails()
        findComment(commentId)
        if (commentService.findLikeByUserIdAndParentId(user.id!!, commentId) == null) {
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user haven't liked this comment $commentId.")
        }
        commentService.removeLike(commentId, user)
        HttpJsonResponse.successResponse(buildCommentResponse(findComment(commentId)), "User's like removed successfully to comment $commentId.")
    }

    fun findTopicAndPost(topicId: String, postId: String) = run {
        topicService.find(topicId) ?: throw ResourceNotFoundException(resourceName = "Topic", resourceId = topicId)
        postService.find(postId) ?: throw ResourceNotFoundException("Post", postId)
    }

    fun findComment(commentId: String) = run {
        commentService.find(commentId) ?: throw ResourceNotFoundException("Comment", commentId)
    }

    fun buildCommentResponse(comment: Comment): CommentResponse = with(comment) {
        CommentResponse(
            id = id!!,
            authorId = author.id,
            content = content,
            parentId = parentId,
            parentType = parentType,
            likes = likes,
            liked = liked,
            createdAt = createdAt!!,
        )
    }

    fun buildCommentIndexDocument(comment: Comment): Document {
        val timestamp: Long? = if (comment.parentType == CommentParentType.POST) {
            postRepository.findByIdOrNull(comment.parentId)?.createdAt?.epochSecond
        } else {
            commentRepository.findByIdOrNull(comment.parentId)?.createdAt?.epochSecond
        }

        if (timestamp == null) {
            throw IllegalStateException("Cannot find parent entity with ID: ${comment.parentId}")
        }
        return Document(comment.id!!).apply {
            put("parentId", comment.parentId)
            put("parentType", comment.parentType.name)
            put("parentCreatedAt", timestamp)
            put("content", comment.content)
            put("authorId", comment.author.id!!)
        }
    }

    private fun buildCommentIndexDocumentUpdate(commentId: String, commentUpdate: CommentUpdate): Document{
        return Document(commentId).apply {
            put("content", commentUpdate.content)
        }
    }
}
