package com.kogo.content.storage.repository

import com.kogo.content.storage.exception.DBAccessException
import com.kogo.content.storage.exception.DocumentNotFoundException
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.findByIdOrNull

inline fun <reified T : Any, ID : Any> MongoRepository<T, ID>.findByIdOrThrow(id: ID): T
    = findByIdOrNull(id) ?: throw DocumentNotFoundException(
        documentType = T::class,
        documentId = id
    )

inline fun <reified T : Any, ID : Any> MongoRepository<T, ID>.saveOrThrow(entity: T)
    = try {
        save(entity)
    } catch (e: RuntimeException) {
        throw DBAccessException(e)
    }

