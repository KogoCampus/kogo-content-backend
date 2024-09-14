package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Post
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository: MongoRepository<Post, String> {
    fun findByTopicId(topicId: String): List<Post>
    fun findByAuthorId(authorId: String): List<Post>
}
