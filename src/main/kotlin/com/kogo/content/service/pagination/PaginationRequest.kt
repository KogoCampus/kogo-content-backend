package com.kogo.content.service.pagination

import io.swagger.v3.oas.annotations.media.Schema

data class PaginationRequest (
    @Schema(type = "string", description = "page token")
    val pageToken: PageToken,

    val limit: Int = 10
) {
    companion object {
        const val PAGE_TOKEN_PARAM = "page_token"
        const val PAGE_SIZE_PARAM = "limit"

        fun resolveFromRequestParameters(requestParameters: Map<String, String>): PaginationRequest {
            val pageTokenParam = requestParameters.getOrDefault(PAGE_TOKEN_PARAM, null)
            val limitParam = requestParameters.getOrDefault(PAGE_SIZE_PARAM, "10").toInt()
            val pageToken = if (pageTokenParam != null) PageToken.fromString(pageTokenParam) else PageToken.create()
            return PaginationRequest(pageToken, limitParam)
        }
    }
}
