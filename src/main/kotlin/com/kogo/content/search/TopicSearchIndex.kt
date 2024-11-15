package com.kogo.content.search

import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import com.kogo.content.storage.view.TopicAggregate
import org.springframework.stereotype.Repository

@Repository
class TopicSearchIndex(
    private val atlasSearchQuery: AtlasSearchQueryBuilder
) : SearchIndex<TopicAggregate> {

    override fun search(
        searchText: String,
        paginationRequest: PaginationRequest,
        boost: Double?
    ): PaginationSlice<TopicAggregate> {
        return atlasSearchQuery.search(
            entityClass = TopicAggregate::class,
            searchIndex = getIndexName(),
            paginationRequest = paginationRequest,
            searchText = searchText,
            searchFields = getSearchFields(),
            scoreFields = listOf(
                AtlasSearchQueryBuilder.ScoreField(
                    field = "followerCount",
                    boost = boost ?: 1.5
                )
            )
        )
    }

    override fun getSearchFields(): List<String> = listOf(
        "name",
        "description",
        "tags"
    )

    override fun getIndexName(): String = "topic_stats_search"

    override fun getCollectionName(): String = "topic_stats"

    override fun getMapping(): SearchMapping = SearchMapping.builder()
        .dynamic(false)
        .addField("name", FieldType.STRING, "lucene.standard")
        .addField("description", FieldType.STRING, "lucene.standard")
        .addField("tags", FieldType.STRING, "lucene.standard")
        .addField("followerCount", FieldType.NUMBER)
        .build()
}
