package com.kogo.content.storage.repository.trait

import com.kogo.content.storage.entity.Like
import com.kogo.content.storage.entity.View
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

interface Likable {

    fun addLike(likableId: String, userId: String): Like?

    fun removeLike(likableId: String, userId: String): Boolean

    fun findLike(likableId: String, userId: String): Like?
}

open class LikableImpl @Autowired constructor(private val mongoTemplate: MongoTemplate) : Likable {

    override fun addLike(likableId: String, userId: String): Like? {
        val existingLike = findLike(likableId, userId)
        if (existingLike != null) { return null }

        val like = Like(
            userId = userId,
            likableId = likableId
        )
        mongoTemplate.insert(like)

        return like
    }

    override fun removeLike(likableId: String, userId: String): Boolean {
        val like = findLike(likableId, userId)
        like?.let { mongoTemplate.remove(like) }
        return true
    }

    override fun findLike(likableId: String, userId: String): Like? {
        val query = Query(
            Criteria.where("likableId").`is`(likableId)
                .and("userId").`is`(userId)
        )
        return mongoTemplate.findOne(query, Like::class.java)
    }
}
