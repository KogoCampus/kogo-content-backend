package com.kogo.content.service.entity

import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse
import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.service.filehandler.FileHandler
import com.kogo.content.storage.entity.*
import com.kogo.content.storage.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PostService (
    private val repository: PostRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandler,
) {
    fun find(postId: String): Post? = repository.findByIdOrNull(postId)

    fun listPostsByTopicId(topicId: String, paginationRequest: PaginationRequest): PaginationResponse<Post> {
        val limit = paginationRequest.limit
        val pageLastResourceId = paginationRequest.pageToken.pageLastResourceId
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id")) as Pageable
        val posts = if (pageLastResourceId != null) repository.findAllByTopicIdAndIdLessThan(topicId, pageLastResourceId, pageable)
                    else repository.findAllByTopicId(topicId, pageable)
        val nextPageToken = posts.lastOrNull()?.let { paginationRequest.pageToken.nextPageToken(it.id!!) }
        return PaginationResponse(posts, nextPageToken)
    }

    fun listPostsByAuthorId(authorId: String): List<Post> = repository.findAllByAuthorId(authorId)

    @Transactional
    fun create(topic: Topic, author: UserDetails, dto: PostDto): Post {
        val post = Post(
            title = dto.title,
            content = dto.content,
            topic = topic,
            author = author,
            attachments = (dto.images!! + dto.videos!!).map {
                attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository)
            },
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        return repository.save(post)
    }

    @Transactional
    fun update(post: Post, postUpdate: PostUpdate) : Post {
        postUpdate.title?.let { post.title = it }
        postUpdate.content?.let { post.content = it }
        post.updatedAt = Instant.now()

        val attachmentMaintainedAfterDeletion: List<Attachment> = post.attachments.filter { attachment -> attachment.id !in postUpdate.attachmentDelete!! }
        val attachmentAdded = (postUpdate.images!! + postUpdate.videos!!).map {
            attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository) }
        post.attachments = attachmentMaintainedAfterDeletion + attachmentAdded
        return repository.save(post)
    }

    @Transactional
    fun delete(post: Post) {
        post.attachments.forEach { attachmentRepository.delete(it) }
        repository.deleteById(post.id!!)
    }

    @Transactional
    fun addLike(post: Post, user: UserDetails) = run {
        repository.addLike(post.id!!, user.id!!)?.let {
            post.likes += 1
            repository.save(post)
            it
        }
    }

    @Transactional
    fun removeLike(post: Post, user: UserDetails) = run {
        if (repository.removeLike(post.id!!, user.id!!)) {
            post.likes -= 1
            repository.save(post)
        }
    }

    @Transactional
    fun addView(post: Post, user: UserDetails) = run {
        repository.addViewCount(post.id!!, user.id!!)?.let {
            post.viewCount += 1
            repository.save(post)
            it
        }
    }

    fun isPostAuthor(post: Post, user: UserDetails): Boolean = post.author == user

    fun hasUserLikedPost(post: Post, user: UserDetails): Boolean = repository.findLike(post.id!!, user.id!!) != null
}
