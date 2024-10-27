package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Like
import com.kogo.content.storage.entity.Reply
import com.kogo.content.storage.repository.traits.UserFeedback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

//class ReplyUserFeedbackImpl : UserFeedback<Reply> {
//
//    @Autowired
//    override lateinit var mongoTemplate: MongoTemplate
//
//    override fun addLike(likableId: String, userId: String): Like? {
//        val like = super.addLike(likableId, userId)
//        if (like != null) {
//            val query = Query(Criteria.where("_id").`is`(likableId))
//            val update = Update().inc("likes", 1)
//            mongoTemplate.updateFirst(query, update, Reply::class.java)
//        }
//        return like
//    }
//
//    override fun removeLike(likableId: String, userId: String): Boolean {
//        val removed = super.removeLike(likableId, userId)
//        if (removed) {
//            val query = Query(Criteria.where("_id").`is`(likableId))
//            val update = Update().inc("likes", -1)
//            mongoTemplate.updateFirst(query, update, Reply::class.java)
//        }
//        return removed
//    }
//}
//
