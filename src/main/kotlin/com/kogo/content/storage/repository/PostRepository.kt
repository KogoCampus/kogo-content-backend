package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Post
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository: MongoRepository<Post, String>, PostRepositoryHandler {
    fun findAllByTopicId(topicId: String): List<Post>
    fun findAllByTopicId(topicId: String, pageable: Pageable): List<Post>
    fun findAllByTopicIdAndIdLessThan(topicId: String, id: String, pageable: Pageable): List<Post>
    fun findAllByAuthorId(authorId: String): List<Post>
}
