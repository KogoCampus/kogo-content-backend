package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Viewer
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class ViewerRepository @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    fun addView(viewableId: String, userId: String): Viewer? {
        val existingViewer = findView(viewableId, userId)
        if (existingViewer != null) return null

        val viewer = Viewer(
            userId = userId,
            viewableId = ObjectId(viewableId)
        )
        return mongoTemplate.insert(viewer)
    }

    fun findView(viewableId: String, userId: String): Viewer? {
        val query = Query(
            Criteria.where("viewableId").`is`(ObjectId(viewableId))
                .and("userId").`is`(userId)
        )
        return mongoTemplate.findOne(query, Viewer::class.java)
    }
}
