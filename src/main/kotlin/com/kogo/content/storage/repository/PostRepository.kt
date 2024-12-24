package com.kogo.content.storage.repository

import com.kogo.content.storage.model.entity.Post
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository: MongoRepository<Post, String> {
    fun findAllByAuthorId(authorId: String): List<Post>
}
