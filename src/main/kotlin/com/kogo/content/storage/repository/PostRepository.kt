package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Post
import org.springframework.data.mongodb.repository.MongoRepository

interface PostRepository: MongoRepository<Post, String> {
    fun findAllById(ids: List<String>): List<Post>
    fun findAllByAuthorId(authorId: String): List<Post>
}
