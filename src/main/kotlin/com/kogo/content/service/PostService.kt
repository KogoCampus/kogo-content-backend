package com.kogo.content.service

import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.storage.entity.*
import com.kogo.content.lib.SortDirection
import com.kogo.content.search.SearchIndex
import com.kogo.content.storage.repository.*
import com.kogo.content.storage.view.PostAggregate
import com.kogo.content.storage.view.PostAggregateView
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PostService (
    private val postRepository: PostRepository,
    private val attachmentRepository: AttachmentRepository,
    private val likeRepository: LikeRepository,
    private val viewerRepository: ViewerRepository,
    private val postAggregateView: PostAggregateView,
    private val postAggregateSearchIndex: SearchIndex<PostAggregate>
) {
    fun find(postId: String) = postRepository.findByIdOrNull(postId)
    fun findAggregate(postId: String) = postAggregateView.find(postId)

    fun findPostsByTopic(topic: Topic, paginationRequest: PaginationRequest)
        = postAggregateView.findAll(paginationRequest.withFilter("topic", topic.id!!))

    fun findPostsByAuthor(user: User) = postRepository.findAllByAuthorId(user.id!!)

    fun findPostAggregatesByLatest(paginationRequest: PaginationRequest)
        = postAggregateView.findAll(
            paginationRequest.withSort("createdAt", SortDirection.DESC)
        )

    fun findPostAggregatesByPopularity(paginationRequest: PaginationRequest) = postAggregateView.findAll(
        paginationRequest.withSort("popularityScore", SortDirection.DESC)
    )

    fun searchPostAggregatesByKeyword(
        searchText: String,
        paginationRequest: PaginationRequest
    ) = postAggregateSearchIndex.search(
        searchText = searchText,
        paginationRequest = paginationRequest,
        boost = 1.0
    )

    fun testFunc(postId: String) {
        postAggregateView.refreshView(postId)
    }

    fun searchPostAggregatesByKeywordAndPopularity(
        searchText: String,
        paginationRequest: PaginationRequest
    ) = postAggregateSearchIndex.search(
        searchText = searchText,
        paginationRequest = paginationRequest,
        boost = 2.0
    )

    @Transactional
    fun create(topic: Topic, author: User, dto: PostDto): Post {
        val savedPost = postRepository.save(
            Post(
                title = dto.title,
                content = dto.content,
                topic = topic,
                author = author,
                attachments = attachmentRepository.saveFiles(dto.images!! + dto.videos!!),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        )
        postAggregateView.refreshView(savedPost.id!!)
        return savedPost
    }

    @Transactional
    fun update(post: Post, postUpdate: PostUpdate): Post {
        postUpdate.title?.let { post.title = it }
        postUpdate.content?.let { post.content = it }
        post.updatedAt = Instant.now()

        val attachmentsToKeep = post.attachments.filter { it.id !in postUpdate.attachmentDelete!! }
        val newAttachments = attachmentRepository.saveFiles(postUpdate.images!! + postUpdate.videos!!)

        post.attachments.filter { it.id in postUpdate.attachmentDelete!! }
            .forEach { attachmentRepository.delete(it) }

        post.attachments = attachmentsToKeep + newAttachments
        val updatedPost = postRepository.save(post)
        postAggregateView.refreshView(post.id!!)
        return updatedPost
    }

    @Transactional
    fun delete(post: Post) {
        post.attachments.forEach { attachmentRepository.delete(it) }
        postRepository.deleteById(post.id!!)
    }

    @Transactional
    fun addLike(post: Post, user: User): Like? {
        val like = likeRepository.addLike(post.id!!, user.id!!)
        if (like != null) {
            postAggregateView.refreshView(post.id!!)
        }
        return like
    }

    @Transactional
    fun removeLike(post: Post, user: User) {
        likeRepository.removeLike(post.id!!, user.id!!)
        postAggregateView.refreshView(post.id!!)
    }

    @Transactional
    fun addViewer(post: Post, user: User): Viewer? {
        val viewer = viewerRepository.addView(post.id!!, user.id!!)
        if (viewer != null) {
            postAggregateView.refreshView(post.id!!)
        }
        return viewer
    }

    fun hasUserLikedPost(post: Post, user: User): Boolean {
        return likeRepository.findLike(post.id!!, user.id!!) != null
    }

    fun hasUserViewedPost(post: Post, user: User): Boolean {
        return viewerRepository.findView(post.id!!, user.id!!) != null
    }

    fun isPostAuthor(post: Post, user: User): Boolean = post.author == user
}
