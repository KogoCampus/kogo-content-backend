package com.kogo.content.service

import com.kogo.content.endpoint.public.model.BaseDto
import com.kogo.content.logging.Logger
import com.kogo.content.storage.entity.MongoEntity
import org.springframework.transaction.annotation.Transactional

interface EntityService<Entity : MongoEntity, DTO : BaseDto> {
    companion object : Logger()
//
//    fun find(documentId: String): Entity?
//
//    @Transactional
//    fun create(dto: DTO): Entity
//
//    fun update(documentId: String, attributes: Map<String, Any?>): Entity?
//
//    @Transactional
//    fun delete(documentId: String): Unit
}