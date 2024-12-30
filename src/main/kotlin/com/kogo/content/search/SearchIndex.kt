package com.kogo.content.search

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass

abstract class SearchIndex<TModel : Any>(val entity: KClass<TModel>) {

    @Autowired
    lateinit var atlasSearchQueryBuilder: AtlasSearchQueryBuilder

    open fun indexName(): String = entity.simpleName!!

    open fun searchIndexDefinition(): SearchIndexDefinition = SearchIndexDefinition.builder().dynamic(true).build()

    open fun mongoEntityCollectionName(): String = entity.simpleName!!.lowercase()

    protected open fun defaultSearchConfiguration(): SearchConfiguration? = null

    fun search(
        searchText: String,
        paginationRequest: PaginationRequest
    ): PaginationSlice<TModel> = atlasSearchQueryBuilder.search(
        entityClass = entity,
        searchIndex = indexName(),
        paginationRequest = paginationRequest,
        searchText = searchText,
        configuration = defaultSearchConfiguration()
    )

    fun search(
        searchText: String,
        paginationRequest: PaginationRequest,
        searchConfiguration: SearchConfiguration
    ): PaginationSlice<TModel> = atlasSearchQueryBuilder.search(
        entityClass = entity,
        searchIndex = indexName(),
        paginationRequest = paginationRequest,
        searchText = searchText,
        configuration = searchConfiguration
    )
}
