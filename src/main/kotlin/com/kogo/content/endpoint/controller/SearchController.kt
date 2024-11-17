package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.endpoint.model.TopicResponse
import com.kogo.content.service.PostService
import com.kogo.content.service.TopicService
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import com.kogo.content.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("media/search")
class SearchController(
    private val postService: PostService,
    private val topicService: TopicService,
    private val userService: UserService
) {

    @GetMapping("posts")
    @Operation(
        summary = "Search posts by keyword",
        parameters = [
            Parameter(
                name = "q",
                description = "Search keyword",
                required = true,
                schema = Schema(type = "string")
            ),
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
            headers = [
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
            ],
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = PostResponse::class))
            )]
        )]
    )
    fun searchPosts(
        @RequestParam("q") keyword: String,
        paginationRequest: PaginationRequest
    ) = run {
        val user = userService.getCurrentUser()
        val paginationResponse = postService.searchPostAggregatesByKeywordAndPopularity(keyword, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { post ->
                PostResponse.create(post, user)
            },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @GetMapping("topics")
    @Operation(
        summary = "Search topics by keyword",
        parameters = [
            Parameter(
                name = "q",
                description = "Search keyword",
                required = true,
                schema = Schema(type = "string")
            ),
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
            headers = [
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
                Header(name = PaginationSlice.HEADER_PAGE_TOKEN, schema = Schema(type = "string")),
            ],
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = TopicResponse::class))
            )]
        )]
    )
    fun searchTopics(
        @RequestParam("q") keyword: String,
        paginationRequest: PaginationRequest
    ) = run {
        val user = userService.getCurrentUser()
        val paginationResponse = topicService.searchTopicAggregatesByKeyword(keyword, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { topic ->
                TopicResponse.create(topic, user)
            },
            headers = paginationResponse.toHttpHeaders()
        )
    }
}
