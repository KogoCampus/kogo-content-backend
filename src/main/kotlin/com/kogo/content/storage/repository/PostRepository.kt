package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Post
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant

interface PostRepository: MongoRepository<Post, String>, PostRepositoryHandler {
    fun findAllByOrderByIdDesc(pageable: Pageable): List<Post>
    fun findAllByIdLessThanOrderByIdDesc(id: String, pageable: Pageable): List<Post>
    fun findAllByViewcountGreaterThanAndCreatedAtAfter(viewCount: Int, createdAt: Instant, pageable: Pageable): List<Post>
    fun findAllByIdLessThanAndViewcountGreaterThanAndCreatedAtAfter(id: String, viewCount: Int, createdAt: Instant, pageable: Pageable): List<Post>
    fun findAllByTopicId(topicId: String): List<Post>
    fun findAllByTopicId(topicId: String, pageable: Pageable): List<Post>
    fun findAllByTopicIdAndIdLessThan(topicId: String, id: String, pageable: Pageable): List<Post>
    fun findAllByOwnerId(authorId: String): List<Post>
}
