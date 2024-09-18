package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.service.PostService
import com.kogo.content.service.LikeService
import com.kogo.content.service.UserContextService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Like
import org.springframework.beans.factory.annotation.Autowired
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("media")
class LikeController @Autowired constructor(
    private val likeService: LikeService,
    private val userService: UserContextService,
    private val postService: PostService
) {
    @RequestMapping(
        path = ["posts/{postId}/likes"],
        method = [RequestMethod.POST]
    )
    @Operation(
        summary = "create a like under the given post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(schema = Schema(implementation = Like::class))]
        )])
    fun createLike(@PathVariable("postId") postId: String): ResponseEntity<*> = run {
        val post = findPostByIdOrThrow(postId)
        val user = userService.getCurrentUserDetails()
        if (likeService.findByUserIdAndParentId(user.id!!, postId) != null)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user already liked this post: ${postId}")
        likeService.create(postId, user)
        HttpJsonResponse.successResponse(buildPostResponse(post), "User's like added successfully to post: ${postId}")
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
        val post = findPostByIdOrThrow(postId)
        val user = userService.getCurrentUserDetails()
        if (likeService.findByUserIdAndParentId(user.id!!, postId) == null)
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "user never liked this post: ${postId}")
        likeService.delete(postId, user)
        HttpJsonResponse.successResponse(buildPostResponse(post), "User's like deleted successfully to post: ${postId}")
    }

    private fun findPostByIdOrThrow(postId: String) = postService.find(postId) ?: throw ResourceNotFoundException.of<Post>(postId)

    private fun buildPostResponse(post: Post): PostResponse = with(post) {
        PostResponse(
            id = id!!,
            topicId = post.topic.id,
            authorUserId = author.id!!,
            title = title,
            content = content,
            attachments = attachments.map { buildPostAttachmentResponse(it) },
            comments = emptyList(), // TODO
            viewcount = viewcount,
            likes = likes
        )
    }

    private fun buildPostAttachmentResponse(attachment: Attachment): PostResponse.PostAttachment = with(attachment) {
        PostResponse.PostAttachment(
            attachmentId = id,
            fileName = fileName,
            url = savedLocationURL,
            contentType = contentType,
            size = fileSize
        )
    }
}
