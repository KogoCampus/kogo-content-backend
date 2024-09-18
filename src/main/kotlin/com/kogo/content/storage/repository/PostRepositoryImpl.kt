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

    override fun updateLikes(postId: String, alreadyLiked: Boolean) {
        val query = Query(Criteria.where("_id").`is`(postId))
        val update = Update()
        println(alreadyLiked)
        if (alreadyLiked) {
            println("dercrementing a like")
            update.inc("likes", -1)  // Decrement if already liked
        } else {
            println("incrementing a like")
            update.inc("likes", 1)   // Increment if not already liked
        }

        mongoTemplate.updateFirst(query, update, Post::class.java)
    }

}
