package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.AttachmentResponse
import com.kogo.content.endpoint.model.OwnerInfoResponse
import com.kogo.content.endpoint.model.PaginationRequest
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.service.FeedService
import com.kogo.content.storage.entity.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("media")
class FeedController @Autowired constructor(
    private val feedService: FeedService
) {
    @GetMapping("feeds/latest")
    @Operation(
        summary = "return a list of latest posts",
        parameters = [Parameter(schema = Schema(implementation = PaginationRequest::class))],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [Header(name = "next_page", schema = Schema(type = "string"))],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)
            )
            )],
        )])
    fun listLatestPosts(
        @RequestParam("limit") limit: Int?,
        @RequestParam("page") page: String?) = run {
        val paginationRequest = if (limit != null) PaginationRequest(limit, page) else PaginationRequest(page = page)
        val paginationResponse = feedService.listFeedsByLatest(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { buildPostResponse(it) },
            headers = paginationResponse.toHeaders()
        )
    }

    @GetMapping("feeds/trending")
    @Operation(
        summary = "return a list of trending posts",
        parameters = [Parameter(schema = Schema(implementation = PaginationRequest::class))],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [Header(name = "next_page", schema = Schema(type = "string"))],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)
            )
            )],
        )])
    fun listTrendingPosts(
        @RequestParam("limit") limit: Int?,
        @RequestParam("page") page: String?) = run {
        val paginationRequest = if (limit != null) PaginationRequest(limit, page) else PaginationRequest(page = page)
        val paginationResponse = feedService.listFeedsByPopular(paginationRequest)
        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { buildPostResponse(it) },
            headers = paginationResponse.toHeaders()
        )
    }

    private fun buildPostResponse(post: Post): PostResponse = with(post) {
        PostResponse(
            id = id!!,
            topicId = post.topic.id,
            owner = buildOwnerInfoResponse(owner),
            title = title,
            content = content,
            attachments = attachments.map { buildAttachmentResponse(it) },
            comments = comments.map { buildPostCommment(it) },
            viewcount = viewcount,
            likes = likes,
            createdAt = createdAt!!,
            commentCount = commentCount,
        )
    }

    private fun buildOwnerInfoResponse(owner: UserDetails): OwnerInfoResponse =  with(owner) {
        OwnerInfoResponse(
            ownerId = id,
            username = username,
            profileImage = profileImage?.let { buildAttachmentResponse(it) },
            schoolShortenedName = schoolShortenedName
        )
    }

    private fun buildAttachmentResponse(attachment: Attachment): AttachmentResponse = with(attachment) {
        AttachmentResponse(
            attachmentId = id,
            name = name,
            url = attachment.storeKey.toFileSourceUrl(),
            contentType = contentType,
            size = fileSize
        )
    }

    private fun buildPostCommment(comment: Comment): PostResponse.PostComment = with(comment) {
        PostResponse.PostComment(
            commentId = id,
            ownerId = buildOwnerInfoResponse(owner),
            replyCount = repliesCount
        )
    }
}
