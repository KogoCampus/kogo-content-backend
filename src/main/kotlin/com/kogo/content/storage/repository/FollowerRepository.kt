package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Follower
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.isEqualTo

@Repository
class FollowerRepository @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    fun follow(followableId: String, userId: String): Follower? {
        val existing = findFollowing(followableId, userId)
        if (existing != null) return null

        val follower = Follower(
            userId = userId,
            followableId = ObjectId(followableId),
        )
        return mongoTemplate.insert(follower)
    }

    fun unfollow(followableId: String, userId: String): Boolean {
        val existing = findFollowing(followableId, userId) ?: return false
        mongoTemplate.remove(existing)
        return true
    }

    fun unfollowAllByFollowableId(followableId: String) {
        val query = Query(
            Criteria.where("followableId").isEqualTo(ObjectId(followableId))
        )
        mongoTemplate.remove(query, Follower::class.java)
    }

    fun findFollowing(followableId: String, userId: String): Follower? {
        val following = Query(
            Criteria.where("userId").`is`(userId)
                .and("followableId").`is`(ObjectId(followableId))
        )
        return mongoTemplate.findOne(following, Follower::class.java)
    }

    fun findAllFollowingsByUserId(userId: String): List<Follower> {
        val followings = Query(Criteria.where("userId").`is`(userId))
        return mongoTemplate.find(followings, Follower::class.java)
    }

    fun findAllFollowingsByFollowableId(followableId: String): List<Follower> {
        val followings = Query(Criteria.where("followableId").`is`(followableId))
        return mongoTemplate.find(followings, Follower::class.java)
    }
}
