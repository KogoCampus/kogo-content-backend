package com.kogo.content.service

import com.kogo.content.endpoint.public.model.GroupDto
import com.kogo.content.logging.Logger
import com.kogo.content.service.exception.UnsupportedMediaTypeException
import com.kogo.content.service.filehandler.FileHandlerService
import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.storage.entity.ProfileImage
import com.kogo.content.storage.repository.GroupRepository
import com.kogo.content.storage.repository.findByIdOrThrow
import com.kogo.content.storage.repository.saveOrThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties


@Service
class GroupService @Autowired constructor(
    private val repository: GroupRepository,
    private val fileHandlerService: FileHandlerService
) : EntityService<GroupEntity, GroupDto> {

    fun find(documentId: String): GroupEntity? {
        return repository.findByIdOrThrow(documentId)
    }

    @Transactional
    fun create(dto: GroupDto): GroupEntity {
        val entity = dto.toEntity()
        validateGroupNameIsUnique(entity.groupName)
        dto.profileImage?.takeIf { !it.isEmpty }?.let {
            entity.profileImage = processProfileImageMultipart(it)
        }
        repository.saveOrThrow(entity)
        return entity
    }

    @Transactional
    fun update(documentId: String, attributes: Map<String, Any?>): GroupEntity? {
        val updatingEntity = repository.findByIdOrThrow(documentId)

        if (attributes.containsKey("groupName") &&
            attributes["groupName"] != null &&
            attributes["groupName"] != updatingEntity.groupName) {
            validateGroupNameIsUnique(attributes["groupName"] as String)
        }

        val properties = GroupEntity::class.memberProperties.associateBy(KProperty<*>::name)
        val mutableAttributes = attributes.toMutableMap()
        mutableAttributes.takeIf { "tags" in it }?.let { it["tags"] = GroupEntity.parseTags(it["tags"] as String) }
        mutableAttributes.takeIf { "profileImage" in it }?.let { it["profileImage"] = processProfileImageMultipart(it["profileImage"] as MultipartFile) }
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
        repository.findByIdOrThrow(documentId)
        repository.deleteById(documentId)
    }

    private fun processProfileImageMultipart(imageFile: MultipartFile): ProfileImage {
        val acceptedMediaTypes = arrayOf(
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE
        )
        imageFile.takeIf { it.contentType in acceptedMediaTypes } ?: throw with(imageFile) {
            val errorMessage = String.format("Invalid media type for profile image. Accepted types are: %s, but provided: %s.",
                acceptedMediaTypes.toString(), contentType)
            UnsupportedMediaTypeException(errorMessage)
        }
        val storeResult = fileHandlerService.store(imageFile)
        return ProfileImage(
            imageUrl = storeResult.url,
            metadata = storeResult.metadata
        )
    }

    private fun validateGroupNameIsUnique(groupName: String) {
        repository.findByGroupName(groupName)?.let {
            throw IllegalArgumentException(String.format("Duplicate key constraint. Group name must be unique. key=%s; value=%s.", "groupName", groupName))
        }
    }

}