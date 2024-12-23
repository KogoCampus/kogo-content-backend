package com.kogo.content.endpoint.common

import org.springframework.http.HttpHeaders

data class PaginationSlice<T>(
    val items: List<T>,
    val nextPageToken: PageToken? = null
) {
    companion object {
        const val HEADER_PAGE_TOKEN = "X-Page-Token"
        const val HEADER_PAGE_SIZE = "X-Page-Size"
    }

    fun toHttpHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        nextPageToken?.let {
            headers.add(HEADER_PAGE_TOKEN, it.encode())
        }
        headers.add(HEADER_PAGE_SIZE, items.size.toString())
        return headers
    }
}
