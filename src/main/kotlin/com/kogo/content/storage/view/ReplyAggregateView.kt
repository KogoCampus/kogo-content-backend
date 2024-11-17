package com.kogo.content.storage.view

import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import com.kogo.content.storage.MongoPaginationQueryBuilder
import com.kogo.content.storage.entity.Reply
import com.kogo.content.storage.view.CommentAggregateView.Companion
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

@Component
class ReplyAggregateView(
    mongoTemplate: MongoTemplate,
    private val mongoPaginationQueryBuilder: MongoPaginationQueryBuilder
) : MongoView<ReplyAggregate>(mongoTemplate, ReplyAggregate::class) {

    companion object {
        private val PAGINATION_FIELD_MAPPINGS = mapOf(
            "id" to "replyId",
            "author" to "reply.author.id",
            "comment" to "reply.comment.id",
            "content" to "reply.content",
            "createdAt" to "reply.createdAt",
            "updatedAt" to "reply.updatedAt"
        )
    }

    fun findAll(paginationRequest: PaginationRequest): PaginationSlice<ReplyAggregate> {
        return mongoPaginationQueryBuilder.getPage(
            ReplyAggregate::class,
            PAGINATION_FIELD_MAPPINGS,
            paginationRequest = paginationRequest
        )
    }

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
