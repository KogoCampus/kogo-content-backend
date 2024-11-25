package com.kogo.content.storage.view

import com.kogo.content.storage.entity.Topic
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

@Component
class TopicAggregateView : MongoView<TopicAggregate>(TopicAggregate::class) {

    override fun buildAggregation(id: String) = newAggregation(
        match(Criteria.where("_id").`is`(id)),

        lookup()
            .from("topic")
            .localField("_id")
            .foreignField("_id")
            .`as`("topic"),
        unwind("topic"),

        // Lookup followers
        lookup()
            .from("follower")
            .localField("_id")
            .foreignField("followableId")
            .`as`("followers"),

        // Lookup posts
        lookup()
            .from("post")
            .localField("_id")
            .foreignField("topic")
            .`as`("posts"),

        // Project final structure
        project()
            .and("_id").`as`("topicId")
            .and("topic").`as`("topic")
            .and("followers.userId").`as`("followerIds")
            .and("followers").size().`as`("followerCount")
            .and("posts._id").`as`("postIds")
            .and("posts").size().`as`("postCount")
            .andExpression("\$\$NOW").`as`("lastUpdated")
    )

    override fun getSourceCollection() = Topic::class.java
}
