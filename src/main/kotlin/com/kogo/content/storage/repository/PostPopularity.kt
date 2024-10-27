package com.kogo.content.storage.repository

import org.springframework.beans.factory.annotation.Autowired
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.repository.traits.UserFeedback
import com.kogo.content.storage.repository.traits.UserFeedbackImpl
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import java.time.Instant
import java.time.temporal.ChronoUnit

interface PostPopularity {
    fun findAllPopular(pageNumber: Number, pageSize: Number): List<Post>
}

class PostPopularityImpl : PostPopularity {

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    override fun findAllPopular(pageNumber: Number, pageSize: Number): List<Post> {
        val offset = pageNumber.toInt() * pageSize.toInt()

        val matchStage = Aggregation.match(
            Criteria.where("_id").gte(Instant.now().minus(2, ChronoUnit.WEEKS)) // within 2 weeks creation time span
        )

        val addPopularityFieldStage = Aggregation.addFields()
            .addField("popularityScore")
            .withValueOf {
                Document ("\$add", listOf(
                    Document("\$multiply", listOf("\$likes", 0.4)),
                    Document("\$multiply", listOf(Document("\$size", "\$comments"), 0.3)),
                    Document("\$multiply", listOf("\$viewCount", 0.4))
                ))
            }
            .build()

        val pipeline = Aggregation.newAggregation(
            matchStage,
            addPopularityFieldStage,
            Aggregation.sort(Sort.by(Sort.Order.desc("popularityScore"))), // Sort by popularity
            Aggregation.skip(offset.toLong()), // Apply offset for pagination
            Aggregation.limit(pageSize.toLong()) // Limit results based on pageSize
        )

        return mongoTemplate.aggregate(pipeline, "posts", Post::class.java).mappedResults
    }
}
