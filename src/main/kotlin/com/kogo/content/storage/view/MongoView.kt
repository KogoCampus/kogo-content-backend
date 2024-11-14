package com.kogo.content.storage.view

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Query
import kotlin.reflect.KClass

abstract class MongoView<T : Any>(
    protected val mongoTemplate: MongoTemplate,
    private val viewClass: KClass<T>
) {
    fun find(id: String): T {
        return mongoTemplate.findById(id, viewClass.java) ?: refreshView(id)
    }

    fun findAll(): List<T> = findAll(Query())

    fun findAll(query: Query): List<T> = mongoTemplate.find(query, viewClass.java)

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
}
