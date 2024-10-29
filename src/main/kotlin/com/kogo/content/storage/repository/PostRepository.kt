package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.repository.trait.Likable
import com.kogo.content.storage.repository.trait.Viewable
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository: MongoRepository<Post, String>, Likable, Viewable, PostPopularity {
    fun findAllByOrderByIdDesc(pageable: Pageable): List<Post>
    fun findAllByIdLessThanOrderByIdDesc(id: String, pageable: Pageable): List<Post>
    fun findAllByTopicId(topicId: String, pageable: Pageable): List<Post>
    fun findAllByTopicIdAndIdLessThan(topicId: String, id: String, pageable: Pageable): List<Post>
    fun findAllByAuthorId(authorId: String): List<Post>
}
