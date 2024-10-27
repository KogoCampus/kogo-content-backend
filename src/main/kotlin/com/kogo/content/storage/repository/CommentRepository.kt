package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.repository.traits.UserFeedback
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository


interface CommentRepository: MongoRepository<Comment, String>, UserFeedback {
    fun findAllByPostId(postId: String): List<Comment>
    fun findAllByPostId(postId: String, pageable: Pageable): List<Comment>
    fun findAllByPostIdAndIdLessThan(postId: String, id: String, pageable: Pageable): List<Comment>
}
