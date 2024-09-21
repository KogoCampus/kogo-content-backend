package com.kogo.content.endpoint.model

import org.springframework.http.HttpHeaders

data class PaginationResponse<T> (
    val items: List<T>,
    val nextPage: String?
) {
    val itemCount: Int
        get() = items.size

    fun toHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.set("next_page", nextPage)
        headers.set("item_count", itemCount.toString())
        return headers
    }
}
