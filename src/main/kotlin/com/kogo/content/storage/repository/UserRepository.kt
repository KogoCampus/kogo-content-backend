package com.kogo.content.storage.repository

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.storage.MongoPaginationQueryBuilder
import com.kogo.content.storage.entity.Notification
import com.kogo.content.storage.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.Criteria

interface UserRepository : MongoRepository<User, String>, UserRepositoryCustom {
    fun findByUsername(username: String): User?

    @Query("{ '_id': ?0 }")
    fun findUserById(id: String): User?
}

interface UserRepositoryCustom {
    fun findUsersByTopicId(topicId: String, paginationRequest: PaginationRequest): PaginationSlice<User>
}

class UserRepositoryCustomImpl : UserRepositoryCustom {

    @Autowired
    lateinit var mongoPaginationQueryBuilder: MongoPaginationQueryBuilder

    override fun findUsersByTopicId(topicId: String, paginationRequest: PaginationRequest): PaginationSlice<User> {
        // Create base aggregation operations to find users following the topic
        val baseAggregation = listOf(
            // Join with followers collection
            Aggregation.lookup(
                "follower",
                "_id",
                "userId",
                "followers"
            ),
            // Match users who follow the topic
            Aggregation.match(
                Criteria.where("followers").ne(null)
                    .and("followers.followableId").`is`(ObjectId(topicId))
            ),
            // Optional: Remove the followers field if you don't need it
            Aggregation.project().andExclude("followers")
        )

        return mongoPaginationQueryBuilder.getPage(
            entityClass = User::class,
            paginationRequest = paginationRequest,
            baseAggregation = baseAggregation
        )
    }
}
