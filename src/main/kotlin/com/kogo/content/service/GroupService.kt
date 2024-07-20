package com.kogo.content.service

import com.kogo.content.endpoint.public.model.GroupDto
import com.kogo.content.exception.DocumentNotFoundException
import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.storage.repository.GroupRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class GroupService (
    private val repository: GroupRepository
) : EntityService<GroupEntity, GroupDto> {

    override fun find(documentId: String): GroupEntity? {
        return repository.findByIdOrNull(documentId) ?: throw DocumentNotFoundException(
            documentType = GroupEntity::class,
            documentId = documentId
        )
    }

    override fun create(dto: GroupDto): GroupEntity {
        val entity = dto.toEntity()
        repository.save(entity)
        return entity
    }

    override fun update(documentId: String, attributes: Map<String, Any?>): GroupEntity? {
        repository.findByIdOrNull(documentId) ?: throw DocumentNotFoundException(
                documentType = GroupEntity::class,
                documentId = documentId
            )
        val updatingEntity = GroupDto.transformer.transformByAttributes(attributes)
        updatingEntity.id = documentId
        return repository.save(updatingEntity)
    }

    fun replace(documentId: String, attributes: Map<String, Any?>) {

    }

    override fun delete(documentId: String) {
        repository.findByIdOrNull(documentId) ?: throw DocumentNotFoundException(
            documentType = GroupEntity::class,
            documentId = documentId
        )
        repository.deleteById(documentId)
    }
}