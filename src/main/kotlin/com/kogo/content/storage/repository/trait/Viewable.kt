package com.kogo.content.storage.repository.trait

import com.kogo.content.storage.entity.View
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

interface Viewable {
    fun addView(viewableId: String, userId: String): View?

    fun removeView(viewableId: String, userId: String): Boolean

    fun findView(viewableId: String, userId: String): View?
}

class ViewableImpl @Autowired constructor(private val mongoTemplate: MongoTemplate) : Viewable {

    override fun addView(viewableId: String, userId: String): View? {
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

    override fun removeView(viewableId: String, userId: String): Boolean {
        val view = findView(viewableId, userId)
        view?.let { mongoTemplate.remove(it) }
        return true
    }

    override fun findView(viewableId: String, userId: String): View? {
        val query = Query(
            Criteria.where("viewableId").`is`(viewableId)
                .and("userId").`is`(userId)
        )
        return mongoTemplate.findOne(query, View::class.java)
    }
}
