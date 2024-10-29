package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.PostResponse
import com.kogo.content.service.PostService
import com.kogo.content.service.pagination.PaginationRequest
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
    private val postService: PostService
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
            Parameter(schema = Schema(implementation = PaginationRequest::class))
        ],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [Header(name = "next_page", schema = Schema(type = "string"))],
            content = [Content(
                mediaType = "application/json",
                array = ArraySchema(schema = Schema(implementation = PostResponse::class))
            )]
        )]
    )
    fun searchPosts(
        @RequestParam("q") keyword: String,
        @RequestParam requestParameters: Map<String, String>
    ) = run {
        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)
        val paginationResponse = postService.searchByKeyword(keyword, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.from(it) },
            headers = paginationResponse.toHttpHeaders()
        )
    }
}
