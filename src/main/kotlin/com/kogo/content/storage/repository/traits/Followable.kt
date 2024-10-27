package com.kogo.content.storage.repository.traits

import com.kogo.content.storage.entity.UserFollowing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

interface Followable {

    fun follow(followableId: String, userId: String): UserFollowing?

    fun unfollow(followableId: String, userId: String): Boolean

    fun unfollowAllByFollowableId(followableId: String)

    fun findFollowing(followableId: String, userId: String): UserFollowing?

    fun findAllFollowingsByUserId(userId: String): List<UserFollowing>

    fun findAllFollowingsByFollowableId(followableId: String): List<UserFollowing>
}

class FollowableImpl : Followable {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    override fun follow(followableId: String, userId: String): UserFollowing? {
        val existing = findFollowing(followableId, userId)
        if (existing != null) { return null }

        val following = UserFollowing(
            userId = userId,
            followableId = followableId,
        )
        mongoTemplate.insert(following)
        return following
    }

    override fun unfollow(followableId: String, userId: String): Boolean {
        val existing = findFollowing(followableId, userId) ?: return false
        mongoTemplate.remove(existing)
        return true
    }

    override fun unfollowAllByFollowableId(followableId: String) {
        val followings = findAllFollowingsByFollowableId(followableId)
        followings.forEach { mongoTemplate.remove(it) }
    }

    override fun findFollowing(followableId: String, userId: String): UserFollowing? {
        val following = Query(
            Criteria.where("userId").`is`(userId)
                .and("followableId").`is`(followableId)
        )
        return mongoTemplate.findOne(following, UserFollowing::class.java)
    }

    override fun findAllFollowingsByUserId(userId: String): List<UserFollowing> {
        val followings = Query(Criteria.where("userId").`is`(userId))
        return mongoTemplate.find(followings, UserFollowing::class.java)
    }

    override fun findAllFollowingsByFollowableId(followableId: String): List<UserFollowing> {
        val followings = Query(Criteria.where("followableId").`is`(followableId))
        return mongoTemplate.find(followings, UserFollowing::class.java)
    }
}
