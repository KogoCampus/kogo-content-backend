package com.kogo.content.service.search

import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationSlice

interface SearchQueryDao<T> {
    fun searchByKeyword(keyword: String, paginationRequest: PaginationRequest): PaginationSlice<T>
}
