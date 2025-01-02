package com.kogo.content.endpoint.common

data class PaginationRequest(
    var pageToken: PageToken = PageToken.create(),
    val limit: Int = 10
) {
    companion object {
        const val PAGE_TOKEN_PARAM = "page_token"
        const val PAGE_SIZE_PARAM = "limit"
        const val SORT_PARAM = "sort"
        const val FILTER_PARAM = "filter"
    }

    fun withFilter(field: String, value: Any, operator: FilterOperator = FilterOperator.EQUALS): PaginationRequest {
        val newFilters = pageToken.filterFields + FilterField(field, value, operator)
        pageToken = pageToken.copy(filterFields = newFilters)
        return this
    }

    fun withSort(field: String, direction: SortDirection = SortDirection.DESC): PaginationRequest {
        val newSortFields = pageToken.sortFields + SortField(field, direction)
        pageToken = pageToken.copy(sortFields = newSortFields)
        return this
    }
}
