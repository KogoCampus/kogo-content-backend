package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.AttachmentResponse
import com.kogo.content.endpoint.model.UserInfo
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.service.entity.FeedService
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
    fun listLatestPosts(@RequestParam requestParameters: Map<String, String>) = run {
        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)
        val paginationResponse = feedService.listPostsByLatest(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.from(it) },
            headers = paginationResponse.toHttpHeaders()
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
    fun listTrendingPosts(@RequestParam requestParameters: Map<String, String>) = run {
        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)
        val paginationResponse = feedService.listPostsByPopularity(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.from(it) },
            headers = paginationResponse.toHttpHeaders()
        )
    }
}
