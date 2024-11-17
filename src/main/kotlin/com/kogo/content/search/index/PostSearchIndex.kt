package com.kogo.content.search.index

import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import com.kogo.content.search.AtlasSearchQueryBuilder
import com.kogo.content.search.ScoreField
import com.kogo.content.search.SearchIndex
import com.kogo.content.search.SearchIndexDefinition
import com.kogo.content.storage.view.PostAggregate
import org.springframework.stereotype.Repository

@Repository
class PostSearchIndex(
    private val atlasSearchQuery: AtlasSearchQueryBuilder
) : SearchIndex<PostAggregate> {

    private val fieldAliases = mapOf(
        "title" to "post.title",
        "content" to "post.content",
        "createdAt" to "post.createdAt",
        "updatedAt" to "post.updatedAt",
        "topic.id" to "post.topic._id"
    )

    override fun search(
        searchText: String,
        paginationRequest: PaginationRequest,
    ): PaginationSlice<PostAggregate> {
        val paginationRequestAliased = SearchIndex.Helper.createAliasedPaginationRequest(
            paginationRequest = paginationRequest,
            fieldAliases = fieldAliases
        )

        return atlasSearchQuery.search(
            entityClass = PostAggregate::class,
            searchIndexName = getIndexName(),
            paginationRequest = paginationRequestAliased,
            searchText = searchText,
            searchableFields = getSearchableFields(),
            scoreFields = listOf(
                ScoreField(
                    field = "post.title",
                    boost = 2.0
                ),
            )
        )
    }

    override fun getSearchableFields(): List<String> = listOf(
        "post.title",
        "post.content"
    )

    override fun getIndexName(): String = "post_stats_search"

    override fun getTargetCollectionName(): String = "post_stats"

    override fun getSearchIndexDefinition(): SearchIndexDefinition = SearchIndexDefinition.builder()
        .dynamic(false)
        .documentField("post") {
            stringField("title")
            stringField("content")
            dateField("createdAt")
            dateField("updatedAt")
        }
        .numberField("popularityScore")
        .numberField("likeCount")
        .numberField("viewCount")
        .numberField("commentCount")
        .dateField("lastUpdated")
        .build()
}
