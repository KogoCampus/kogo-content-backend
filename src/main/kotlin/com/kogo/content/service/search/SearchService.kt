package com.kogo.content.service.search

import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse

interface SearchService {
    fun searchByKeyword(keyword: String): List<*>

    fun searchByKeyword(keyword: String, paginationRequest: PaginationRequest): PaginationResponse<*>
}
