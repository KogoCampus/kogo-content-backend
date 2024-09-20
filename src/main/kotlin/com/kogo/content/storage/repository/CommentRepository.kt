package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Comment
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query


interface CommentRepository: MongoRepository<Comment, String>, CommentRepositoryCustom {
    @Query("{'parentId': ?0}")
    fun findAllByParentId(parentId: String): List<Comment>

}
