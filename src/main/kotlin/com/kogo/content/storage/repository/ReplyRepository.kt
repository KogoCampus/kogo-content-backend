package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Reply
import com.kogo.content.storage.repository.traits.UserFeedback
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ReplyRepository : MongoRepository<Reply, String>, UserFeedback {
    fun findAllByCommentId(commentId: String, pageable: Pageable): List<Reply>
    fun findAllByCommentIdAndIdLessThan(commentId: String, id: String, pageable: Pageable): List<Reply>
}
