package com.kogo.content.service.pagination

import org.springframework.http.HttpHeaders

data class PaginationResponse<T> (
    val items: List<T>,
    val nextPage: PageToken?
) {
    private val itemCount: Int
        get() = items.size

    companion object {
        const val HEADER_NAME_PAGE_TOKEN = "X-Page-Token"
        const val HEADER_NAME_PAGE_SIZE = "X-Page-Size"
    }

    fun toHttpHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.set(HEADER_NAME_PAGE_TOKEN, nextPage?.toString())
        headers.set(HEADER_NAME_PAGE_SIZE, itemCount.toString())
        return headers
    }
}
