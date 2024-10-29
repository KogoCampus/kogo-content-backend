package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Reply
import com.kogo.content.storage.repository.trait.Likable
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ReplyRepository : MongoRepository<Reply, String>, Likable {
    fun findAllByCommentId(commentId: String, pageable: Pageable): List<Reply>
    fun findAllByCommentIdAndIdLessThan(commentId: String, id: String, pageable: Pageable): List<Reply>
    fun deleteAllByCommentId(commentId: String)
}
