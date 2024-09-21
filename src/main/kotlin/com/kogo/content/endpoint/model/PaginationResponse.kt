package com.kogo.content.endpoint.model

data class PaginationResponse<T> (
    val items: List<T>,
    val nextPage: String?
)
