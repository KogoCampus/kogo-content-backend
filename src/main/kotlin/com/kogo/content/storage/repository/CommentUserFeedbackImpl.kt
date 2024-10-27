package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.Like
import com.kogo.content.storage.repository.traits.UserFeedback
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

//class CommentUserFeedbackImpl(override var mongoTemplate: MongoTemplate) : UserFeedback<Comment> {
//
//    override fun addLike(likableId: String, userId: String): Like? {
//        val like = super.addLike(likableId = likableId, userId = userId)
//        if (like != null) {
//            val query = Query(Criteria.where("_id").`is`(likableId))
//            val update = Update().inc("likes", 1)
//            mongoTemplate.updateFirst(query, update, Comment::class.java)
//        }
//        return like
//    }
//
//    override fun removeLike(likableId: String, userId: String): Boolean {
//        val removed = super.removeLike(likableId = likableId, userId = userId)
//        if (removed) {
//            val query = Query(Criteria.where("_id").`is`(likableId))
//            val update = Update().inc("likes", -1)
//            mongoTemplate.updateFirst(query, update, Comment::class.java)
//        }
//        return removed;
//    }
//}
//
