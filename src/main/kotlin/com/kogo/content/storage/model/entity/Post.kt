package com.kogo.content.storage.model.entity

import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.Comment
import com.kogo.content.storage.model.Like
import org.bson.Document
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.mapping.DocumentReference
import com.kogo.content.storage.model.RecencyScorer
import java.time.Instant

@org.springframework.data.mongodb.core.mapping.Document
data class Post (
    @Id
    var id : String? = null,

    @DocumentReference
    var group: Group,

    @DocumentReference
    var author: User,

    var title: String,

    var content: String,

    var comments: MutableList<Comment> = mutableListOf(),

    var attachments: MutableList<Attachment> = mutableListOf(),

    // full likes history, regardless currently active or inactive
    var likes: MutableList<Like> = mutableListOf(),

    var viewerIds: MutableList<String> = mutableListOf(),

    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
) {
    val activeLikes: List<Like>
        get() = likes.filter { it.isActive }

    companion object {
        private val recencyScorer = RecencyScorer.create(
            maxValue = 1.0,
            steepness = 0.5,
            threshold = java.time.Duration.ofDays(7)
        )

        fun addPopularityAggregationOperations(): List<AggregationOperation> {
            val currentDate = Instant.now()
            val recencyPipeline = recencyScorer.toAggregationPipeline(
                timestampField = "createdAt",
                currentTime = currentDate
            )

            return listOf(
                // Add recency score operations
                *recencyPipeline.map { doc ->
                    Aggregation.addFields().addField(doc.getString("\$addFields")).withValue(doc).build()
                }.toTypedArray(),

                // Calculate likes score
                Aggregation.addFields().addField("likesScore").withValue(
                    Document("\$size", Document("\$filter", Document()
                        .append("input", "\$likes")
                        .append("as", "like")
                        .append("cond", Document("\$eq", listOf("\$\$like.isActive", true)))
                    ))
                ).build(),

                // Calculate comments and replies score
                Aggregation.addFields().addField("commentsScore").withValue(
                    Document("\$sum", Document("\$map", Document()
                        .append("input", "\$comments")
                        .append("as", "comment")
                        .append("in", Document("\$add", listOf(
                            1, // Each comment counts as 1
                            Document("\$size", "\$\$comment.replies")
                        )))
                    ))
                ).build(),

                // Calculate total popularity score
                Aggregation.addFields().addField("popularityScore").withValue(
                    Document("\$add", listOf(
                        Document("\$multiply", listOf(0.4, "\$recencyScore")),
                        Document("\$multiply", listOf(0.3, "\$likesScore")),
                        Document("\$multiply", listOf(0.3, "\$commentsScore"))
                    ))
                ).build()
            )
        }
    }
}
