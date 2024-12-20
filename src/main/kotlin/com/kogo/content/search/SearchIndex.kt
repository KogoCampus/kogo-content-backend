package com.kogo.content.search

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice

interface SearchIndex<T : Any> {

    class Helper {
        companion object {
            fun createAliasedPaginationRequest(
                paginationRequest: PaginationRequest,
                fieldAliases: Map<String, String>
            ): PaginationRequest {
                val mappedSortFields = paginationRequest.pageToken.sortFields.map { sortField ->
                    sortField.copy(field = fieldAliases[sortField.field] ?: sortField.field)
                }

                val mappedFilters = paginationRequest.pageToken.filters.map { filter ->
                    filter.copy(field = fieldAliases[filter.field] ?: filter.field)
                }

                return paginationRequest.copy(
                    pageToken = paginationRequest.pageToken.copy(
                        sortFields = mappedSortFields,
                        filters = mappedFilters
                    )
                )
            }
        }
    }

    fun search(
        searchText: String,
        paginationRequest: PaginationRequest,
        configOverride: SearchConfiguration? = null
    ): PaginationSlice<T>

    fun getIndexName(): String
    fun getTargetCollectionName(): String
    fun getSearchIndexDefinition(): SearchIndexDefinition = SearchIndexDefinition()
    fun getSearchConfiguration(): SearchConfiguration
}
