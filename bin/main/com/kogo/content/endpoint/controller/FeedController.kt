package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.model.GroupResponse
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.logging.Logger
import com.kogo.content.service.GroupService
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
    private val userService: UserService,
    private val groupService: GroupService
) {
    @GetMapping("feeds/trendingPosts")
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
        val user = userService.findCurrentUser()
        val paginationResponse = postService.findAllTrending(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.from(it, user) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @GetMapping("feeds/latestPosts")
    @Operation(
        summary = "return a list of latest posts in following groups",
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
        val user = userService.findCurrentUser()
        val paginationResponse = postService.findAllInFollowing(paginationRequest, user)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.from(it, user) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @GetMapping("feeds/trendingGroups")
    @Operation(
        summary = "return a list of trending groups",
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
                schema = Schema(implementation = GroupResponse::class)
            ))],
        )]
    )
    fun listTrendingGroups(paginationRequest: PaginationRequest) = run {
        val user = userService.findCurrentUser()
        val paginationResponse = groupService.findAllTrending(paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { GroupResponse.from(it, user) },
            headers = paginationResponse.toHttpHeaders()
        )
    }
}
