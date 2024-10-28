package com.kogo.content.storage.repository.traits

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

    fun addViewCount(viewableId: String, userId: String): View?
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

    override fun addViewCount(viewableId: String, userId: String): View? {
        val existingView = mongoTemplate.findOne(
            Query(
                Criteria.where("userId").`is`(userId)
                    .and("viewableId").`is`(viewableId)
            ), View::class.java)
        if (existingView != null) { return null }

        val view = View(
            userId = userId,
            viewableId = viewableId
        )
        mongoTemplate.insert(view)

        return view
    }
}
