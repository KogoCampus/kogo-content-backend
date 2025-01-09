package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.endpoint.model.GroupResponse
import com.kogo.content.service.PostService
import com.kogo.content.service.GroupService
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
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
    private val groupService: GroupService,
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
                Header(name = PaginationSlice.HEADER_PAGE_SIZE, schema = Schema(type = "string")),
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
        val user = userService.findCurrentUser()
        val paginationResponse = postService.search(keyword, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { it ->
                PostResponse.from(it, user)
            },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @GetMapping("groups")
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
                Header(name = PaginationSlice.HEADER_PAGE_SIZE, schema = Schema(type = "string")),
            ],
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = GroupResponse::class))
            )]
        )]
    )
    fun searchTopics(
        @RequestParam("q") keyword: String,
        paginationRequest: PaginationRequest
    ) = run {
        val user = userService.findCurrentUser()
        val paginationResponse = groupService.search(keyword, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { GroupResponse.from(it, user) },
            headers = paginationResponse.toHttpHeaders()
        )
    }
}
