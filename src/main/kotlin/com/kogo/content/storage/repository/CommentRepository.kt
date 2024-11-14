package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.MongoPaginationQueryBuilder
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

interface CommentRepository: MongoRepository<Comment, String> {
    fun deleteAllByPostId(postId: String)
}
