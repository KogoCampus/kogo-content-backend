package com.kogo.content.service

import com.kogo.content.endpoint.public.model.GroupDto
import com.kogo.content.logging.Logger
import com.kogo.content.service.exception.UnsupportedMediaTypeException
import com.kogo.content.service.filehandler.FileHandler
import com.kogo.content.storage.exception.DocumentNotFoundException
import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.storage.entity.ProfileImage
import com.kogo.content.storage.exception.DBAccessException
import com.kogo.content.storage.repository.GroupRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties


@Service
class GroupService @Autowired constructor(
    private val repository: GroupRepository,
    private val fileHandler: FileHandler
) : EntityService<GroupEntity, GroupDto> {

    companion object : Logger()

    override fun find(documentId: String): GroupEntity? {
        return repository.findByIdOrNull(documentId) ?: throw DocumentNotFoundException(
            documentType = GroupEntity::class,
            documentId = documentId
        )
    }

    override fun create(dto: GroupDto): GroupEntity {
        val entity = dto.toEntity()

        checkGroupNameIsUnique(entity.groupName)
        dto.profileImage?.takeIf { !it.isEmpty }?.let {
            entity.profileImage = processProfileImageMultipart(it)
        }
        repository.save(entity)
        return entity
    }

    override fun update(documentId: String, attributes: Map<String, Any?>): GroupEntity? {
        val updatingEntity = repository.findByIdOrNull(documentId) ?: throw DocumentNotFoundException(
                documentType = GroupEntity::class,
                documentId = documentId
            )

        if (attributes.containsKey("groupName") &&
            attributes["groupName"] != null &&
            attributes["groupName"] != updatingEntity.groupName) {
            checkGroupNameIsUnique(attributes["groupName"] as String)
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
        return repository.save(updatingEntity)
    }

    override fun delete(documentId: String) {
        repository.findByIdOrNull(documentId) ?: throw DocumentNotFoundException(
            documentType = GroupEntity::class,
            documentId = documentId
        )
        repository.deleteById(documentId)
    }

    private fun checkGroupNameIsUnique(groupName: String) {
        repository.findByGroupName(groupName)?.let {
            throw IllegalArgumentException(String.format("Unique key constraint error; key=%s; value=%s", "groupName", groupName))
        }
    }

    private fun processProfileImageMultipart(imageFile: MultipartFile): ProfileImage {
        val acceptedMediaTypes = arrayOf(
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE
        )
        imageFile.takeIf { it.contentType in acceptedMediaTypes } ?: throw with(imageFile) {
            val errorMessage = String.format("Error: Invalid profile image file type. Accepted types: %s, but provided: %s.",
                acceptedMediaTypes.toString(), contentType)
            UnsupportedMediaTypeException(errorMessage)
        }
        val storeResult = fileHandler.store(imageFile)
        return ProfileImage(
            imageUrl = storeResult.url,
            metadata = storeResult.metadata
        )
    }
}