package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Comment
import org.springframework.data.mongodb.repository.MongoRepository

interface CommentRepository: MongoRepository<Comment, String> {
    fun deleteAllByPostId(postId: String)
}
