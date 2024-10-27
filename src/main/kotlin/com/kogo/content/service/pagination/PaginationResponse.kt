package com.kogo.content.service.pagination

import org.springframework.http.HttpHeaders

data class PaginationResponse<T> (
    val items: List<T>,
    val nextPage: PageToken?
) {
    private val itemCount: Int
        get() = items.size

    fun toHttpHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.set("next_page_token", nextPage?.toString())
        headers.set("page_item_count", itemCount.toString())
        return headers
    }
}
