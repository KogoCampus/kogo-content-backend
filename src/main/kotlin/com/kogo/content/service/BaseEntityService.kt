package com.kogo.content.service

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.common.SortDirection
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.storage.pagination.MongoPaginationQueryBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull
import kotlin.reflect.KClass

abstract class BaseEntityService<TModel : Any, TID>(
    protected val entity: KClass<TModel>,
    protected val repository: MongoRepository<TModel, TID>
) {
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var mongoPaginationQueryBuilder: MongoPaginationQueryBuilder

    fun find(id: TID): TModel? = repository.findByIdOrNull(id)

    fun findOrThrow(id: TID): TModel = find(id) ?: throw ResourceNotFoundException(entity.simpleName!!, id.toString())

    fun findAll(paginationRequest: PaginationRequest): PaginationSlice<TModel> =
        mongoPaginationQueryBuilder.getPage(
            entityClass = entity,
            paginationRequest = paginationRequest.withSort("createdAt", SortDirection.DESC)
        )
}
