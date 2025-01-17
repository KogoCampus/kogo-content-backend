package com.kogo.content.service

import com.kogo.content.endpoint.model.GroupDto
import com.kogo.content.endpoint.model.GroupUpdate
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.search.index.GroupSearchIndex
import com.kogo.content.service.NotificationService.Companion.log
import com.kogo.content.storage.model.entity.Follower
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.repository.*
import com.kogo.content.storage.model.entity.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.multipart.MultipartFile

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val groupSearchIndex: GroupSearchIndex
) : BaseEntityService<Group, String>(Group::class, groupRepository) {

    @Value("\${kogo-api.uploadFiles}")
    lateinit var fileUploaderUrl: String
    val restTemplate = RestTemplate()

    fun findByGroupName(groupName: String): Group? = groupRepository.findByGroupName(groupName)

    fun findByOwner(owner: User) = groupRepository.findAllByOwnerId(owner.id!!)

    fun findAllByFollowerId(userId: String): List<Group> = groupRepository.findAllByFollowerId(userId)

    // TODO: trending groups, temporarily set to find all
    fun findAllTrending(paginationRequest: PaginationRequest) = findAll(paginationRequest)

    fun search(
        searchKeyword: String,
        paginationRequest: PaginationRequest
    ): PaginationSlice<Group> {
        return groupSearchIndex.search(
            searchText = searchKeyword,
            paginationRequest = paginationRequest,
        )
    }

    @Transactional
    fun create(dto: GroupDto, owner: User): Group {
        val profileImage = dto.profileImage?.let { uploadImage(it) }
        return groupRepository.save(
            Group(
                groupName = dto.groupName,
                description = dto.description,
                owner = owner,
                profileImage = profileImage,
                tags = dto.tags.toMutableList()
            )
        )
    }

    @Transactional
    fun update(group: Group, groupUpdate: GroupUpdate): Group {
        with(groupUpdate) {
            groupName?.let { group.groupName = it }
            description?.let { group.description = it }
            tags?.let { group.tags = it.toMutableList() }
            profileImage?.let {
                group.profileImage?.let { profileImage ->
                    deleteImage(profileImage.id!!)
                }
                group.profileImage = uploadImage(it)
            }
        }
        group.updatedAt = System.currentTimeMillis()
        return groupRepository.save(group)
    }

    @Transactional
    fun delete(group: Group) {
        group.profileImage?.let { profileImage ->
            deleteImage(profileImage.id!!)
        }
        groupRepository.deleteById(group.id!!)
    }

    @Transactional
    fun follow(group: Group, user: User): Boolean {
        if (group.followers.any { it.follower.id == user.id })
            return false
        group.followers.add(Follower(user))
        user.followingGroupIds.add(group.id!!)
        groupRepository.save(group)
        userRepository.save(user)
        return true
    }

    @Transactional
    fun unfollow(group: Group, user: User): Boolean {
        if (group.followers.any { it.follower.id == user.id }) {
            group.followers.removeIf { it.follower.id == user.id }
            user.followingGroupIds.remove(group.id!!)
            groupRepository.save(group)
            userRepository.save(user)
            return true
        }
        return false
    }

    fun transferOwnership(group: Group, user: User): Group {
        group.owner = user
        groupRepository.save(group)
        return group
    }

    private fun uploadImage(profileImage: MultipartFile): Attachment? {
        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("file", profileImage.resource)
        }
        val requestEntity = HttpEntity(bodyBuilder.build(), headers)

        return try {
            val response = restTemplate.exchange(
                "${fileUploaderUrl}/images", // URL for file upload
                HttpMethod.POST,
                requestEntity,
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )

            // Check if the upload was successful
            if (response.statusCode.is2xxSuccessful) {
                log.info { "Profile image uploaded successfully" }

                // Get the response body and create the Attachment object
                val responseBody = response.body ?: throw IllegalStateException("Empty response body")

                val imageId = responseBody["imageId"] as? String ?: throw IllegalStateException("Image ID is missing")
                val filename = responseBody["filename"] as? String ?: throw IllegalStateException("Filename is missing")
                val url = responseBody["url"] as? String ?: throw IllegalStateException("URL is missing")
                val contentType = responseBody["content_type"] as? String ?: throw IllegalStateException("Content type is missing")
                val size = (responseBody["size"] as? Number)?.toLong() ?: throw IllegalStateException("Size is missing")

                // Return the Attachment object with extracted values
                Attachment(
                    id = imageId,
                    filename = filename,
                    url = url,
                    contentType = contentType,
                    size = size
                )
            } else {
                log.error { "Failed to upload profile image, status code: ${response.statusCode}" }
                null
            }
        } catch (ex: Exception) {
            log.error(ex) { "Error while uploading profile image: ${ex.message}" }
            null
        }
    }

    private fun deleteImage(imageId: String){
        try{
            val response = restTemplate.exchange(
                "$fileUploaderUrl/images/$imageId", // URL for file delete
                HttpMethod.DELETE,
                null, // No body or headers required
                object : ParameterizedTypeReference<Map<String, Any>>() {}
            )
            if (response.statusCode.is2xxSuccessful) {
                log.info { "Successfully deleted image with ID: $imageId" }
            } else {
                log.error { "Failed to delete image with ID: $imageId, status code: ${response.statusCode}" }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Error while deleting image with ID: $imageId: ${ex.message}" }
            throw IllegalStateException("Failed to delete image: ${ex.message}", ex)
        }
    }
}
