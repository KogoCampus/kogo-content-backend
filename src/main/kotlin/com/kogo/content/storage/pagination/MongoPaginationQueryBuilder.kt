package com.kogo.content.storage.pagination

import com.kogo.content.exception.InvalidFieldException
import com.kogo.content.endpoint.common.*
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation

@Component
class MongoPaginationQueryBuilder(
    private val mongoTemplate: MongoTemplate
) {
    fun <T : Any> getPage(
        entityClass: KClass<T>,
        paginationRequest: PaginationRequest,
        preAggregationOperations: List<AggregationOperation> = emptyList()
    ): PaginationSlice<T> {
        validateFields(paginationRequest, entityClass, entityClass.simpleName ?: "Unknown")

        val operations = mutableListOf<AggregationOperation>()
        operations.addAll(preAggregationOperations)
        operations.addAll(buildPaginationOperations(paginationRequest))

        val results = mongoTemplate.aggregate(
            Aggregation.newAggregation(operations),
            entityClass.java,
            entityClass.java
        ).mappedResults

        return if (results.size > paginationRequest.limit) {
            val lastItem = results[paginationRequest.limit - 1]
            val nextPageToken = buildNextPageToken(
                lastItem,
                paginationRequest.pageToken.sortFields,
                paginationRequest.pageToken.filterFields
            )
            PaginationSlice(
                items = results.take(paginationRequest.limit),
                nextPageToken = nextPageToken
            )
        } else {
            PaginationSlice(items = results)
        }
    }

    private fun buildPaginationOperations(
        request: PaginationRequest
    ): List<AggregationOperation> {
        val operations = mutableListOf<AggregationOperation>()

        // Apply filters
        val filterCriteria = buildFilterCriteria(request.pageToken)
        if (filterCriteria != null) {
            operations.add(Aggregation.match(filterCriteria))
        }

        // Apply cursor-based pagination
        if (request.pageToken.cursors.isNotEmpty()) {
            val cursorCriteria = buildCursorCriteria(request.pageToken)
            operations.add(Aggregation.match(cursorCriteria))
        }

        // Apply sort
        val sortFields = request.pageToken.sortFields.map {
            Sort.Order(
                if (it.direction == SortDirection.ASC) Sort.Direction.ASC else Sort.Direction.DESC,
                it.field
            )
        }
        if (sortFields.isNotEmpty()) {
            operations.add(Aggregation.sort(Sort.by(sortFields)))
        }

        // Apply limit
        operations.add(Aggregation.limit(request.limit + 1L))

        return operations
    }

    private fun buildFilterCriteria(pageToken: PageToken): Criteria? {
        if (pageToken.filterFields.isEmpty()) return null

        val criteria = pageToken.filterFields.map { filter ->
            val fieldName = filter.field
            when (filter.operator) {
                FilterOperator.EQUALS -> Criteria.where(fieldName).`is`(filter.value)
                FilterOperator.IN -> {
                    @Suppress("UNCHECKED_CAST")
                    val values = when (filter.value) {
                        is List<*> -> filter.value
                        else -> listOf(filter.value)
                    }
                    Criteria.where(fieldName).`in`(values)
                }
                FilterOperator.LESS_THAN -> Criteria.where(fieldName).lt(filter.value)
                FilterOperator.GREATER_THAN -> Criteria.where(fieldName).gt(filter.value)
            }
        }

        return Criteria().andOperator(*criteria.toTypedArray())
    }

    private fun buildCursorCriteria(pageToken: PageToken): Criteria {
        val sortFields = pageToken.sortFields
        if (sortFields.isEmpty()) return Criteria()

        val cursors = pageToken.cursors
        val conditions = mutableListOf<Criteria>()

        for (i in sortFields.indices) {
            val condition = mutableListOf<Criteria>()
            val field = sortFields[i].field
            val cursorValue = cursors[field]

            if (cursorValue != null) {
                val comparisonValue = cursorValue.toTypedValue()
                val currentCriteria = if (sortFields[i].direction == SortDirection.ASC) {
                    Criteria.where(field).gt(comparisonValue)
                } else {
                    Criteria.where(field).lt(comparisonValue)
                }
                condition.add(currentCriteria)
            }

            if (condition.isNotEmpty()) {
                conditions.add(Criteria().andOperator(*condition.toTypedArray()))
            }
        }

        return if (conditions.isEmpty()) Criteria()
        else Criteria().orOperator(*conditions.toTypedArray())
    }

    private fun <T : Any> buildNextPageToken(
        lastItem: T,
        sortFields: List<SortField>,
        filterFields: List<FilterField>,
    ): PageToken {
        val cursors = sortFields.associate { sortField ->
            val field = sortField.field
            val document = mongoTemplate.converter.convertToMongoType(lastItem) as org.bson.Document
            val value = getFieldValueInDocument(document, field)
            sortField.field to CursorValue.from(value)
        }
        return PageToken(cursors = cursors, sortFields = sortFields, filterFields = filterFields)
    }

    // Check if field exists in the entity class
    private fun validateFields(
        request: PaginationRequest,
        entityClass: KClass<*>,
        entityName: String
    ) {
        // Check sort fields
        request.pageToken.sortFields.forEach { sortField ->
            if (!isFieldValid(sortField.field, entityClass)) {
                throw InvalidFieldException(sortField.field, entityName, "sorting")
            }
        }

        // Check filter fields
        request.pageToken.filterFields.forEach { filter ->
            if (!isFieldValid(filter.field, entityClass)) {
                throw InvalidFieldException(filter.field, entityName, "filtering")
            }
        }

        // Check cursor fields
        request.pageToken.cursors.keys.forEach { field ->
            if (!isFieldValid(field, entityClass)) {
                throw InvalidFieldException(field, entityName, "cursor pagination")
            }
        }
    }

    // Check if field exists in the entity class
    private fun <T : Any> isFieldValid(
        field: String,
        entityClass: KClass<T>
    ): Boolean {
        return try {
            entityClass.java.getDeclaredField(field)
            true
        } catch (e: NoSuchFieldException) {
            false
        }
    }

    private fun getFieldValueInDocument(document: org.bson.Document, field: String): Any {
        val fields = field.split(".")
        var value: Any = document
        for (f in fields) {
            value = (value as? org.bson.Document)?.get(f)
                ?: throw IllegalArgumentException("Field not found: $field")
        }
        return value
    }
}

