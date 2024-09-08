package com.kogo.content.service

import com.kogo.content.endpoint.model.GroupDto
import com.kogo.content.searchengine.MeilisearchService
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.storage.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties


@Service
class TopicService @Autowired constructor(
    private val repository: TopicRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentService: AttachmentService,
    private val authenticatedUserService: AuthenticatedUserService,
    private val meilisearchService: MeilisearchService
) : EntityService<TopicEntity, GroupDto> {

    fun find(documentId: String): TopicEntity? {
        return repository.findByIdOrThrow(documentId)
    }

    @Transactional
    fun create(dto: GroupDto): TopicEntity {
        val entity = dto.toEntity()
        validateGroupNameIsUnique(entity.groupName)
        val newGroup = repository.saveOrThrow(entity)

        val profileImage = dto.profileImage?.takeIf { !it.isEmpty }?.let {
            attachmentService.saveAttachment(it, newGroup.id)
        }
        newGroup.profileImage = profileImage

        //TO BE DELETED/MODIFIED
        //START
        val owner = authenticatedUserService.findUser("testUser")
        newGroup.owner = owner
        //END

        meilisearchService.indexGroup(newGroup)

        return repository.saveOrThrow(newGroup)
    }

    @Transactional
    fun update(documentId: String, attributes: Map<String, Any?>): TopicEntity? {
        val updatingEntity = repository.findByIdOrThrow(documentId)

        if (attributes.containsKey("groupName") &&
            attributes["groupName"] != null &&
            attributes["groupName"] != updatingEntity.groupName) {
            validateGroupNameIsUnique(attributes["groupName"] as String)
        }

        val properties = TopicEntity::class.memberProperties.associateBy(KProperty<*>::name)
        val mutableAttributes = attributes.toMutableMap()
        mutableAttributes.takeIf { "tags" in it }?.let { it["tags"] = TopicEntity.parseTags(it["tags"] as String) }
        mutableAttributes.takeIf { "profileImage" in it }?.let {
            val newProfileImageFile = it["profileImage"] as MultipartFile
            val newAttachment = attachmentService.saveAttachment(newProfileImageFile, documentId)

            updatingEntity.profileImage?.let { existingAttachment ->
                existingAttachment.parent = null
                attachmentRepository.save(existingAttachment)
            }

            it["profileImage"] = newAttachment
        }
        mutableAttributes.forEach { (name, value) ->
            properties[name]
                .takeIf { it is KMutableProperty<*> }
                ?.let { it as KMutableProperty<*> }
                ?.let { it.setter.call(updatingEntity, value) }
        }
        return repository.saveOrThrow(updatingEntity)
    }

    @Transactional
    fun delete(documentId: String) {
        val group = repository.findByIdOrThrow(documentId)
        group.profileImage?.id?.let { profileImageId ->
            attachmentRepository.findById(profileImageId).ifPresent { attachment ->
                attachment.parent = null
                attachmentRepository.save(attachment)
            }
        }
        meilisearchService.deleteGroup(group.id)
        repository.deleteById(documentId)
    }

    private fun validateGroupNameIsUnique(groupName: String) {
        repository.findByGroupName(groupName)?.let {
            throw IllegalArgumentException(String.format("Duplicate key constraint. Group name must be unique. key=%s; value=%s.", "groupName", groupName))
        }
    }

}