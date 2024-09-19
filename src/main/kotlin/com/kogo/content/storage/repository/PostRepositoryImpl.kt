package com.kogo.content.storage.repository

import org.springframework.beans.factory.annotation.Autowired
import com.kogo.content.storage.entity.Post
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class PostRepositoryImpl : PostRepositoryCustom {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    override fun addLike(postId: String) {
        val query = Query(Criteria.where("_id").`is`(postId))
        val update = Update()
        update.inc("likes", 1)
        mongoTemplate.updateFirst(query, update, Post::class.java)
    }

    override fun removeLike(postId: String) {
        val query = Query(Criteria.where("_id").`is`(postId))
        val update = Update()
        update.inc("likes", -1)
        mongoTemplate.updateFirst(query, update, Post::class.java)
    }

    override fun addView(postId: String) {
        val query = Query(Criteria.where("_id").`is`(postId))
        val update = Update()
        update.inc("viewcount", 1)
        mongoTemplate.updateFirst(query, update, Post::class.java)
    }

}
