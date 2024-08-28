package com.kogo.content.service

import com.kogo.content.endpoint.public.model.PostDto
import com.kogo.content.storage.entity.PostEntity
import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.storage.entity.UserEntity
import com.kogo.content.storage.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.data.DefaultRepositoryTagsProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class PostService @Autowired constructor(
    private val repository: PostRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentService: AttachmentService,
    private val groupService: GroupService,
    private val userService: UserService,
) : EntityService<PostEntity, PostDto> {

    fun findPostsbyGroupId(groupId: String): List<PostEntity>? {
        checkGroupExists(groupId)
        return repository.findByGroupId(groupId)
    }

    fun find(groupId:String, postId: String): PostEntity?{
        checkGroupExists(groupId)
        return repository.findByIdOrThrow(postId)
    }

    @Transactional
    fun create(groupId: String, dto: PostDto): PostEntity {
        val entity = dto.toEntity()

        // check if group exists
        val groupEntity = checkGroupExists(groupId)

        entity.group = groupEntity
        val savedPost = repository.save(entity)

        val attachments = dto.attachments?.map { file ->
            attachmentService.saveAttachment(file, savedPost.id)
        } ?: emptyList()

        savedPost.attachments = attachments

        //TO BE DELETED/MODIFIED
        //START
        val author = userService.findUser("testUser")
        savedPost.author = author
        //END
        return repository.save(savedPost)
    }

    @Transactional
    fun update(groupId:String, postId: String, attributes: Map<String, Any?>) : PostEntity? {
        checkGroupExists(groupId)
        val updatingEntity = repository.findByIdOrThrow(postId)
        attributes.forEach { (name, value) ->
            when (name) {
                "title" -> if (value is String) updatingEntity.title = value
                "content" -> if (value is String) updatingEntity.content = value
            }
        }
        return repository.save(updatingEntity)
    }

    @Transactional
    fun delete(postId: String) {
        val post = repository.findByIdOrThrow(postId)
        post.attachments?.forEach { file ->
            val attachmentToDelete = file.id?.let { attachmentRepository.findById(it) }
            attachmentToDelete?.ifPresent { attachment ->
                attachment.parent = null
                attachmentRepository.save(attachment)
            }
        }
        repository.deleteById(postId)
    }

    @Transactional
    fun addAttachment(postId: String, attachmentFile: MultipartFile) {
        val updatingEntity = repository.findByIdOrThrow(postId)
        val attachment = attachmentService.saveAttachment(attachmentFile, postId)
        val attachments = updatingEntity.attachments?.plus(attachment)
        updatingEntity.attachments = attachments
        repository.save(updatingEntity)
    }

    @Transactional
    fun deleteAttachment(postId: String, attachmentId: String){
        val post = repository.findByIdOrThrow(postId)
        val attachmentToDelete = attachmentRepository.findByIdOrThrow(attachmentId)

        val updatedAttachments = post.attachments?.filter { it.id != attachmentId }

        post.attachments = updatedAttachments
        attachmentToDelete.parent = null

        repository.save(post)
        attachmentRepository.save(attachmentToDelete)
    }

    private fun checkGroupExists(groupId: String) : GroupEntity {
        return groupService.find(groupId)
            ?: throw IllegalArgumentException("Group not found for id: $groupId")
    }
}