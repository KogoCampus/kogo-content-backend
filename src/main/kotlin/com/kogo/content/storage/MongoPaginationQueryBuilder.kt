package com.kogo.content.storage

import com.kogo.content.exception.InvalidFieldException
import com.kogo.content.common.*
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
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
        baseAggregation: List<AggregationOperation> = emptyList(),
        fieldMappings: Map<String, String> = emptyMap(),
        excludedFields: Set<String> = emptySet()
    ): PaginationSlice<T> {
        validateFields(paginationRequest, fieldMappings, excludedFields, entityClass, entityClass.simpleName ?: "Unknown")

        // Build the aggregation pipeline
        val operations = mutableListOf<AggregationOperation>()
        operations.addAll(baseAggregation)

        val paginationOperations = buildPaginationOperations(fieldMappings, paginationRequest)
        operations.addAll(paginationOperations)

        val aggregation = Aggregation.newAggregation(operations)
        val results = mongoTemplate.aggregate(aggregation, entityClass.java, entityClass.java).mappedResults

        return if (results.size > paginationRequest.limit) {
            val lastItem = results[paginationRequest.limit - 1]
            val nextPageToken = buildNextPageToken(
                lastItem,
                paginationRequest.pageToken.sortFields,
                fieldMappings
            ).copy(
                sortFields = paginationRequest.pageToken.sortFields,
                filters = paginationRequest.pageToken.filters
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
        fieldMappings: Map<String, String>,
        request: PaginationRequest
    ): List<AggregationOperation> {
        val operations = mutableListOf<AggregationOperation>()

        // Apply filters
        val filterCriteria = buildFilterCriteria(request.pageToken, fieldMappings)
        if (filterCriteria != null) {
            operations.add(Aggregation.match(filterCriteria))
        }

        // Apply cursor-based pagination
        if (request.pageToken.cursors.isNotEmpty()) {
            val cursorCriteria = buildCursorCriteria(request.pageToken, fieldMappings)
            operations.add(Aggregation.match(cursorCriteria))
        }

        // Apply sort
        val sortFields = request.pageToken.sortFields.map {
            Sort.Order(
                if (it.direction == SortDirection.ASC) Sort.Direction.ASC else Sort.Direction.DESC,
                fieldMappings[it.field] ?: it.field
            )
        }
        if (sortFields.isNotEmpty()) {
            operations.add(Aggregation.sort(Sort.by(sortFields)))
        }

        // Apply limit
        operations.add(Aggregation.limit(request.limit + 1L))

        return operations
    }

    private fun buildFilterCriteria(pageToken: PageToken, fieldMappings: Map<String, String>): Criteria? {
        if (pageToken.filters.isEmpty()) return null

        val criteria = pageToken.filters.map { filter ->
            val fieldName = fieldMappings[filter.field] ?: filter.field
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
            }
        }

        return Criteria().andOperator(*criteria.toTypedArray())
    }

    private fun buildCursorCriteria(pageToken: PageToken, fieldMappings: Map<String, String>): Criteria {
        val sortFields = pageToken.sortFields
        if (sortFields.isEmpty()) return Criteria()

        val cursors = pageToken.cursors
        val conditions = mutableListOf<Criteria>()

        for (i in sortFields.indices) {
            val condition = mutableListOf<Criteria>()
            val field = sortFields[i].field
            val mappedField = fieldMappings[field] ?: field
            val cursorValue = cursors[field]

            if (cursorValue != null) {
                val comparisonValue = cursorValue.toTypedValue()
                val currentCriteria = if (sortFields[i].direction == SortDirection.ASC) {
                    Criteria.where(mappedField).gt(comparisonValue)
                } else {
                    Criteria.where(mappedField).lt(comparisonValue)
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

    private fun getFieldValue(document: org.bson.Document, field: String): Any {
        val fields = field.split(".")
        var value: Any = document
        for (f in fields) {
            value = (value as? org.bson.Document)?.get(f)
                ?: throw IllegalArgumentException("Field not found: $field")
        }
        return value
    }

    fun <T : Any> buildNextPageToken(
        lastItem: T,
        sortFields: List<SortField>,
        fieldMappings: Map<String, String>
    ): PageToken {
        val cursors = sortFields.associate { sortField ->
            val field = fieldMappings[sortField.field] ?: sortField.field
            val document = mongoTemplate.converter.convertToMongoType(lastItem) as org.bson.Document
            val value = getFieldValue(document, field)
            sortField.field to CursorValue.from(value)
        }
        return PageToken(cursors = cursors)
    }

    private fun validateFields(
        request: PaginationRequest,
        fieldMappings: Map<String, String>,
        excludedFields: Set<String>,
        entityClass: KClass<*>,
        entityName: String
    ) {
        // Check sort fields
        request.pageToken.sortFields.forEach { sortField ->
            if (sortField.field in excludedFields) {
                throw InvalidFieldException(sortField.field, entityName, "sorting")
            }
            if (!isFieldValid(sortField.field, fieldMappings, entityClass)) {
                throw InvalidFieldException(sortField.field, entityName, "sorting")
            }
        }

        // Check filter fields
        request.pageToken.filters.forEach { filter ->
            if (filter.field in excludedFields) {
                throw InvalidFieldException(filter.field, entityName, "filtering")
            }
            if (!isFieldValid(filter.field, fieldMappings, entityClass)) {
                throw InvalidFieldException(filter.field, entityName, "filtering")
            }
        }

        // Check cursor fields
        request.pageToken.cursors.keys.forEach { field ->
            if (field in excludedFields) {
                throw InvalidFieldException(field, entityName, "cursor pagination")
            }
            if (!isFieldValid(field, fieldMappings, entityClass)) {
                throw InvalidFieldException(field, entityName, "cursor pagination")
            }
        }
    }

    private fun <T : Any> isFieldValid(
        field: String,
        fieldMappings: Map<String, String>,
        entityClass: KClass<T>
    ): Boolean {
        // Check if field is explicitly mapped
        if (field in fieldMappings.keys) return true

        // Check if field exists in the entity class
        return try {
            entityClass.java.getDeclaredField(field)
            true
        } catch (e: NoSuchFieldException) {
            false
        }
    }
}

