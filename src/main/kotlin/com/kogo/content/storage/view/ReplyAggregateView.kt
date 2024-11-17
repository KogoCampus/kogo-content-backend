package com.kogo.content.storage.view

import com.kogo.content.storage.entity.Reply
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

@Component
class ReplyAggregateView : MongoView<ReplyAggregate>(ReplyAggregate::class) {

    override fun fieldAlias(): Map<String, String> = mapOf(
        "author" to "reply.author.id",
        "comment" to "reply.comment.id",
        "content" to "reply.content",
        "createdAt" to "reply.createdAt",
        "updatedAt" to "reply.updatedAt"
    )

    override fun buildAggregation(id: String) = newAggregation(
        match(Criteria.where("_id").`is`(id)),

        lookup()
            .from("reply")
            .localField("_id")
            .foreignField("_id")
            .`as`("reply"),
        unwind("reply"),

        lookup()
            .from("like")
            .localField("_id")
            .foreignField("likableId")
            .`as`("likes"),

        project()
            .and("_id").`as`("replyId")
            .and("reply").`as`("reply")
            .and("likes.userId").`as`("likedUserIds")
            .and("likes").size().`as`("likeCount")
            .andExpression("\$\$NOW").`as`("lastUpdated")
    )

    override fun getSourceCollection() = Reply::class.java
}
