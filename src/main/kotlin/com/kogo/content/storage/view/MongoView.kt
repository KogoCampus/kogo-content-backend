package com.kogo.content.storage.view

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.storage.MongoPaginationQueryBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Query
import kotlin.reflect.KClass

abstract class MongoView<T : Any>(
    private val viewClass: KClass<T>
) {
    @Autowired
    protected lateinit var mongoTemplate: MongoTemplate

    @Autowired
    protected lateinit var mongoPaginationQueryBuilder: MongoPaginationQueryBuilder

    fun find(id: String): T {
        return mongoTemplate.findById(id, viewClass.java) ?: refreshView(id)
    }

    fun findAll(): List<T> = findAll(Query())

    fun findAll(query: Query): List<T> = mongoTemplate.find(query, viewClass.java)

    fun findAll(paginationRequest: PaginationRequest): PaginationSlice<T> {
        return mongoPaginationQueryBuilder.getPage(
            viewClass,
            fieldAlias(),
            paginationRequest = paginationRequest
        )
    }

    fun refreshView(id: String): T {
        return mongoTemplate.aggregate(
            buildAggregation(id),
            getSourceCollection(),
            viewClass.java
        ).uniqueMappedResult?.also {
            mongoTemplate.save(it)
        } ?: throw RuntimeException("Failed to refresh view for ${viewClass.simpleName} with id: $id")
    }

    protected abstract fun buildAggregation(id: String): Aggregation
    protected abstract fun getSourceCollection(): Class<*>

    protected open fun fieldAlias(): Map<String, String> = emptyMap()
}