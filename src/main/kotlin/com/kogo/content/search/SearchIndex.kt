package com.kogo.content.search

import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice

interface SearchIndex<T : Any> {
    fun search(
        searchText: String,
        paginationRequest: PaginationRequest,
        boost: Double? = null
    ): PaginationSlice<T>

    fun getSearchFields(): List<String>
    fun getIndexName(): String
    fun getCollectionName(): String
    fun getMapping(): SearchMapping = SearchMapping()
}
