package com.kogo.content.service

import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.repository.*
import com.kogo.content.service.util.Transformer
import com.kogo.content.service.util.deleteAttachment
import com.kogo.content.service.util.saveFileAndConvertToAttachment
import com.kogo.content.storage.entity.UserDetails
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService (
    private val repository: PostRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandler,
) {
    private val transformer: Transformer<PostDto, Post> = object : Transformer<PostDto, Post>(PostDto::class, Post::class) {}

    fun find(postId: String): Post? = repository.findByIdOrNull(postId)

    fun listPostsByTopicId(topicId: String): List<Post> = repository.findByTopicId(topicId)

    fun listPostsByAuthorId(authorId: String): List<Post> = repository.findByAuthorId(authorId)

    @Transactional
    fun create(topic: Topic, author: UserDetails, dto: PostDto): Post {
        val post = transformer.transform(dto)
        post.topic = topic
        post.author = author

        val attachments = (dto.images!! + dto.videos!!).map {
            saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) }
        post.attachments = attachments
        return repository.save(post)
    }

    @Transactional
    fun update(post: Post, postUpdate: PostUpdate) : Post {
        postUpdate.title?.let { post.title = it }
        postUpdate.content?.let { post.content = it }

        val attachmentsUpdated: List<Attachment> = post.attachments.filter { attachment -> attachment.id !in postUpdate.attachmentDelete!! }
        val attachmentAdded = (postUpdate.images!! + postUpdate.videos!!).map {
            saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) }
        post.attachments = attachmentsUpdated + attachmentAdded
        return repository.save(post)
    }

    @Transactional
    fun delete(post: Post) {
        post.attachments.forEach { deleteAttachment(it, attachmentRepository) }
        repository.deleteById(post.id!!)
    }
}
