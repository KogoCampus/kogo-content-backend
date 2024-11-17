package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Reply
import org.springframework.data.mongodb.repository.MongoRepository

interface ReplyRepository : MongoRepository<Reply, String> {
    fun deleteAllByCommentId(commentId: String)
}
