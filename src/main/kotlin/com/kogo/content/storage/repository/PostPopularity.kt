package com.kogo.content.storage.repository

import org.springframework.beans.factory.annotation.Autowired
import com.kogo.content.storage.entity.Post
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.TypedAggregation
import org.springframework.data.mongodb.core.query.Criteria
import java.time.Instant
import java.time.temporal.ChronoUnit

interface PostPopularity {
    fun findAllPopular(pageNumber: Number, pageSize: Number): List<Post>
}

class PostPopularityImpl @Autowired constructor(private val mongoTemplate: MongoTemplate) : PostPopularity {

    override fun findAllPopular(pageNumber: Number, pageSize: Number): List<Post> {
        val offset = pageNumber.toInt() * pageSize.toInt()
        val twoWeeksAgo = Instant.now().minus(14, ChronoUnit.DAYS)

        val matchStage = Aggregation.match(
            Criteria.where("createdAt").gte(twoWeeksAgo)
        )

        val addPopularityFieldStage = Aggregation.addFields()
            .addField("popularityScore")
            .withValue(
                Document("\$add", listOf(
                    Document("\$multiply", listOf(
                        Document("\$ifNull", listOf("\$likes", 0)),
                        0.4
                    )),
                    Document("\$multiply", listOf(
                        Document("\$ifNull", listOf("\$commentCount", 0)),
                        0.3
                    )),
                    Document("\$multiply", listOf(
                        Document("\$ifNull", listOf("\$viewCount", 0)),
                        0.3
                    ))
                ))
            )
            .build()

        val operations = listOf(
            matchStage,
            addPopularityFieldStage,
            Aggregation.sort(Sort.by(Sort.Order.desc("popularityScore"))),
            Aggregation.skip(offset.toLong()),
            Aggregation.limit(pageSize.toLong())
        )

        val typedAggregation = TypedAggregation(
            Post::class.java,
            operations
        )

        return mongoTemplate.aggregate(typedAggregation, Post::class.java).mappedResults
    }
}
