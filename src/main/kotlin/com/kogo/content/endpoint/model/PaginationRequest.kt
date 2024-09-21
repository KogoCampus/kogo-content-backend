package com.kogo.content.endpoint.model

data class PaginationRequest (
    val limit: Int = 10,
    val page: String?
)
