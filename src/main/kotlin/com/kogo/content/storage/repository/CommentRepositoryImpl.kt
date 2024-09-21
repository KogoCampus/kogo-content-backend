package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Comment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class CommentRepositoryImpl : CommentRepositoryCustom {
    @Autowired private lateinit var mongoTemplate: MongoTemplate

    override fun addLike(commentId: String) {
        val query = Query(Criteria.where("_id").`is`(commentId))
        val update = Update()
        update.inc("likes", 1)
        mongoTemplate.updateFirst(query, update, Comment::class.java)
    }

    override fun removeLike(commentId: String) {
        val query = Query(Criteria.where("_id").`is`(commentId))
        val update = Update()
        update.inc("likes", -1)
        mongoTemplate.updateFirst(query, update, Comment::class.java)
    }
}
