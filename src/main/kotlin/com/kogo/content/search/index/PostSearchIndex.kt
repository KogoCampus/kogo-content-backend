package com.kogo.content.search.index

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.search.*
import com.kogo.content.storage.view.PostAggregate
import org.springframework.stereotype.Repository
import java.util.Date

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

    override fun getSearchConfiguration() = SearchConfiguration(
        textSearchFields = listOf("post.title", "post.content"),
        nearFields = listOf(
            DateNearField(
                field = "post.createdAt",
                origin = Date(),
                pivot = DateNearField.ONE_WEEK_MS,  // 7 days
            ),
        ),
        scoreFields = listOf(
            ScoreField(
                field = "post.title",
                score = Score.Boost(1.5),
            )
        )
    )

    override fun search(
        searchText: String,
        paginationRequest: PaginationRequest,
        configOverride: SearchConfiguration?
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
            configuration = configOverride ?: getSearchConfiguration()
        )
    }

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
