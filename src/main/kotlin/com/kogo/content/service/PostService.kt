package com.kogo.content.service

import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.PostEntity
import com.kogo.content.storage.entity.UserDetailsEntity
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.storage.repository.*
import com.kogo.content.service.util.Transformer
import com.kogo.content.service.util.deleteAttachment
import com.kogo.content.service.util.saveFileAndConvertToAttachment
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService (
    private val repository: PostRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandler,
) {
    private val transformer: Transformer<PostDto, PostEntity> = object : Transformer<PostDto, PostEntity>(PostDto::class, PostEntity::class) {}

    fun find(postId: String): PostEntity? = repository.findByIdOrNull(postId)

    fun listPostsByTopicId(topicId: String): List<PostEntity> = repository.findByTopicId(topicId)

    fun listPostsByAuthorId(authorId: String): List<PostEntity> = repository.findByAuthor(authorId)

    @Transactional
    fun create(topic: TopicEntity, author: String, dto: PostDto): PostEntity {
        val post = transformer.transform(dto)
        post.topic = topic
        post.author = author

        val attachments = (dto.images!! + dto.videos!!).map {
            saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) }
        post.attachments = attachments
        return repository.save(post)
    }

    @Transactional
    fun update(post: PostEntity, postUpdate: PostUpdate) : PostEntity {
        postUpdate.title?.let { post.title = it }
        postUpdate.content?.let { post.content = it }

        val attachmentsUpdated: List<Attachment> = post.attachments.filter { attachment -> attachment.id !in postUpdate.attachmentDelete!! }
        val attachmentAdded = (postUpdate.images!! + postUpdate.videos!!).map {
            saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) }
        post.attachments = attachmentsUpdated + attachmentAdded
        return repository.save(post)
    }

    @Transactional
    fun delete(post: PostEntity) {
        post.attachments.forEach { deleteAttachment(it, attachmentRepository) }
        repository.deleteById(post.id!!)
    }
}
