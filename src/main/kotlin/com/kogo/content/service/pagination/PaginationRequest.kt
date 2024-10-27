package com.kogo.content.service.pagination

import io.swagger.v3.oas.annotations.media.Schema

data class PaginationRequest (
    @Schema(type = "string", description = "page token")
    val pageToken: PageToken,

    val limit: Int = 10
) {
    companion object {
        fun resolveFromRequestParameters(requestParameters: Map<String, String>): PaginationRequest {
            val pageTokenValue = requestParameters.getOrDefault("pageToken", null)
            val limitValue = requestParameters.getOrDefault("limit", "10").toInt()
            val pageToken = PageToken(pageTokenValue)
            return PaginationRequest(pageToken, limitValue)
        }
    }
}
