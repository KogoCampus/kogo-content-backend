package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.MongoPaginationQueryBuilder
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

interface PostRepository: MongoRepository<Post, String> {
    fun findAllById(ids: List<String>): List<Post>
    fun findAllByAuthorId(authorId: String): List<Post>
}
