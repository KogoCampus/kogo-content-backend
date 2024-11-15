package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Like
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class LikeRepository @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    fun addLike(likableId: String, userId: String): Like? {
        val existingLike = findLike(likableId, userId)
        if (existingLike != null) return null

        val like = Like(
            userId = userId,
            likableId = ObjectId(likableId)
        )
        return mongoTemplate.save(like)
    }

    fun removeLike(likableId: String, userId: String): Boolean {
        val like = findLike(likableId, userId)
        like?.let { mongoTemplate.remove(it) }
        return true
    }

    fun findLike(likableId: String, userId: String): Like? {
        val query = Query(
            Criteria.where("likableId").`is`(likableId)
                .and("userId").`is`(userId)
        )
        return mongoTemplate.findOne(query, Like::class.java)
    }
}
