package com.kogo.content.search.index

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.search.*
import com.kogo.content.storage.entity.User
import org.springframework.stereotype.Repository

@Repository
class UserSearchIndex(
    private val atlasSearchQuery: AtlasSearchQueryBuilder
) : SearchIndex<User> {

    override fun getSearchConfiguration() = SearchConfiguration(
        textSearchFields = listOf("username"),
    )

    override fun search(
        searchText: String,
        paginationRequest: PaginationRequest,
        configOverride: SearchConfiguration?
    ): PaginationSlice<User> {
        return atlasSearchQuery.search(
            entityClass = User::class,
            searchIndexName = getIndexName(),
            paginationRequest = paginationRequest,
            searchText = searchText,
            configuration = configOverride ?: getSearchConfiguration()
        )
    }

    override fun getIndexName(): String = "user_search"

    override fun getTargetCollectionName(): String = "user"

    override fun getSearchIndexDefinition(): SearchIndexDefinition = SearchIndexDefinition.builder()
        .dynamic(false)
        .stringField("username")
        .stringField("schoolName")
        .stringField("schoolShortenedName")
        .build()
}
