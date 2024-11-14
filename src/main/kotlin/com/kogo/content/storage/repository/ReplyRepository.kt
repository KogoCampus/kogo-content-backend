package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Reply
import com.kogo.content.storage.MongoPaginationQueryBuilder
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

interface ReplyRepository : MongoRepository<Reply, String> {
    fun deleteAllByCommentId(commentId: String)
}
