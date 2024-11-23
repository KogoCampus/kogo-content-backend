package com.kogo.content.search.index

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.search.*
import com.kogo.content.storage.view.TopicAggregate
import org.springframework.stereotype.Repository

@Repository
class TopicSearchIndex(
    private val atlasSearchQuery: AtlasSearchQueryBuilder
) : SearchIndex<TopicAggregate> {

    private val fieldAliases = mapOf(
        "name" to "topic.topicName",
        "description" to "topic.description",
        "tags" to "topic.tags",
        "createdAt" to "topic.createdAt",
        "updatedAt" to "topic.updatedAt"
    )

    override fun getSearchConfiguration() = SearchConfiguration(
        textSearchFields = listOf(
            "topic.topicName",
            "topic.description",
            "topic.tags"
        ),
        scoreFields = listOf(
            ScoreField(
                field = "topic.topicName",
                score = Score.Boost(1.25)
            ),
            ScoreField(
                field = "topic.tags",
                score = Score.Boost(1.25)
            )
        )
    )

    override fun search(
        searchText: String,
        paginationRequest: PaginationRequest,
        configOverride: SearchConfiguration?
    ): PaginationSlice<TopicAggregate> {
        val paginationRequestAliased = SearchIndex.Helper.createAliasedPaginationRequest(
            paginationRequest = paginationRequest,
            fieldAliases = fieldAliases
        )

        return atlasSearchQuery.search(
            entityClass = TopicAggregate::class,
            searchIndexName = getIndexName(),
            paginationRequest = paginationRequestAliased,
            searchText = searchText,
            configuration = configOverride ?: getSearchConfiguration()
        )
    }

    override fun getIndexName(): String = "topic_stats_search"

    override fun getTargetCollectionName(): String = "topic_stats"

    override fun getSearchIndexDefinition(): SearchIndexDefinition = SearchIndexDefinition.builder()
        .dynamic(false)
        .documentField("topic") {
            stringField("topicName")
            stringField("description")
            stringField("tags")
            dateField("createdAt")
            dateField("updatedAt")
        }
        .numberField("followerCount")
        .numberField("postCount")
        .dateField("lastUpdated")
        .build()
}
