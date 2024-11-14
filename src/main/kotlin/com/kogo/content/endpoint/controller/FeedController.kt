package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.service.PostService
import com.kogo.content.service.UserService
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
    private val postService: PostService,
    private val userService: UserService
) {
    @GetMapping("feeds/latest")
    @Operation(
        summary = "return a list of latest posts",
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
            )
        ],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [Header(name = "next_page", schema = Schema(type = "string"))],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)
            ))],
        )]
    )
    fun listLatestPosts(paginationRequest: PaginationRequest) = run {
        val user = userService.getCurrentUser()
        val paginationResponse = postService.findPostAggregatesByLatest(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { post ->
                PostResponse.create(post, user)
            },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @GetMapping("feeds/trending")
    @Operation(
        summary = "return a list of trending posts",
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
            )
        ],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [Header(name = "next_page", schema = Schema(type = "string"))],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class)
            ))],
        )]
    )
    fun listTrendingPosts(paginationRequest: PaginationRequest) = run {
        val user = userService.getCurrentUser()
        val paginationResponse = postService.findPostAggregatesByPopularity(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { post ->
                PostResponse.create(post, user)
            },
            headers = paginationResponse.toHttpHeaders()
        )
    }
}
