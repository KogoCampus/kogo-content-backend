package com.kogo.content.storage.view

import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import com.kogo.content.storage.MongoPaginationQueryBuilder
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.view.PostAggregateView.Companion
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

@Component
class CommentAggregateView(
    mongoTemplate: MongoTemplate,
    private val mongoPaginationQueryBuilder: MongoPaginationQueryBuilder
) : MongoView<CommentAggregate>(mongoTemplate, CommentAggregate::class) {

    companion object {
        private val PAGINATION_FIELD_MAPPINGS = mapOf(
            "id" to "_id",
            "author" to "comment.author.id",
            "post" to "comment.post.id",
            "content" to "comment.content",
            "createdAt" to "comment.createdAt",
            "updatedAt" to "comment.updatedAt",
        )
    }

    fun findAll(paginationRequest: PaginationRequest): PaginationSlice<CommentAggregate> {
        return mongoPaginationQueryBuilder.getPage(
            CommentAggregate::class,
            PAGINATION_FIELD_MAPPINGS,
            paginationRequest = paginationRequest
        )
    }

    override fun buildAggregation(id: String) = newAggregation(
        match(Criteria.where("_id").`is`(id)),

        lookup()
            .from("comment")
            .localField("_id")
            .foreignField("_id")
            .`as`("comment"),
        unwind("comment"),

        lookup()
            .from("like")
            .localField("_id")
            .foreignField("likableId")
            .`as`("likes"),

        lookup()
            .from("reply")
            .localField("_id")
            .foreignField("comment")
            .`as`("replies"),

        project()
            .and("_id").`as`("commentId")
            .and("comment").`as`("comment")
            .and("likes.userId").`as`("likedUserIds")
            .and("likes").size().`as`("likeCount")
            .and("replies").size().`as`("replyCount")
            .andExpression("\$\$NOW").`as`("lastUpdated")
    )

    override fun getSourceCollection() = Comment::class.java
}
