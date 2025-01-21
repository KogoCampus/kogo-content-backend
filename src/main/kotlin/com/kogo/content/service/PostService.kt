package com.kogo.content.service

import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.SortDirection
import com.kogo.content.endpoint.model.CommentUpdate
import com.kogo.content.exception.FileOperationFailureException
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.logging.Logger
import com.kogo.content.search.SearchIndex
import com.kogo.content.storage.model.Comment
import com.kogo.content.storage.model.Like
import com.kogo.content.storage.model.Reply
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.repository.*
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val postSearchIndex: SearchIndex<Post>,
    private val fileService: FileUploaderService
) : BaseEntityService<Post, String>(Post::class, postRepository) {
    companion object : Logger()

    fun findCommentOrThrow(postId: String, commentId: String): Comment {
        val post = findOrThrow(postId)
        return post.comments.find { it.id.toString() == commentId }
            ?: throw ResourceNotFoundException.of<Comment>(commentId)
    }

    fun findReplyOrThrow(postId: String, commentId: String, replyId: String): Reply {
        val comment = findCommentOrThrow(postId, commentId)
        return comment.replies.find { it.id.toString() == replyId }
            ?: throw ResourceNotFoundException.of<Reply>(replyId)
    }

    fun findPostsByGroup(group: Group, paginationRequest: PaginationRequest)
        = mongoPaginationQueryBuilder.getPage(
            entityClass = Post::class,
            paginationRequest = paginationRequest
                .withFilter("group", ObjectId(group.id!!))
                .withSort("createdAt", SortDirection.DESC)
        )

    fun findAllByAuthor(user: User) = postRepository.findAllByAuthorId(user.id!!)

    fun findAllLatestInFollowing(paginationRequest: PaginationRequest, user: User) = run {
        val eightDaysAgo = System.currentTimeMillis() - java.time.Duration.ofDays(8).toMillis()
        val matchOperation = Aggregation.match(
            Criteria.where("group").`in`(user.followingGroupIds.map { ObjectId(it) })
                .and("createdAt").gte(eightDaysAgo)
        )

        mongoPaginationQueryBuilder.getPage(
            entityClass = Post::class,
            paginationRequest = paginationRequest.withSort("createdAt", SortDirection.DESC),
            preAggregationOperations = listOf(matchOperation)
        )
    }

    fun findAllTrending(paginationRequest: PaginationRequest, user: User) = run {
        val preAggregationOperations = mutableListOf<AggregationOperation>()

        // Add match operation to filter groups
        val matchOperation = Aggregation.match(
            Criteria().orOperator(
                Criteria.where("group.isSchoolGroup").exists(false),
                Criteria.where("group.id").`is`(ObjectId(user.schoolInfo.schoolGroupId))
            )
        )
        preAggregationOperations.add(matchOperation)

        // Add popularity scoring operations
        preAggregationOperations.addAll(Post.addPopularityAggregationOperations())

        mongoPaginationQueryBuilder.getPage(
            entityClass = Post::class,
            paginationRequest = paginationRequest.withSort("popularityScore", SortDirection.DESC),
            preAggregationOperations = preAggregationOperations,
            allowedDynamicFields = setOf("popularityScore")
        )
    }

    fun search(
        searchKeyword: String,
        paginationRequest: PaginationRequest
    ) = postSearchIndex.search(
        searchText = searchKeyword,
        paginationRequest = paginationRequest,
    )

    @Transactional
    fun create(group: Group, author: User, dto: PostDto): Post {
        val attachments = dto.images?.map{
            fileService.uploadImage(it)
        } ?: emptyList()
        val savedPost = postRepository.save(
            Post(
                title = dto.title,
                content = dto.content,
                group = group,
                author = author,
                images = attachments.toMutableList(),
            )
        )
        return savedPost
    }

    @Transactional
    fun update(post: Post, postUpdate: PostUpdate): Post {
        postUpdate.title?.let { post.title = it }
        postUpdate.content?.let { post.content = it }
        post.updatedAt = System.currentTimeMillis()

        // Images
        val imagesToKeep = post.images.filter { it.id !in postUpdate.attachmentDeleteIds!! }
        val imagesToDelete = post.images.filter{ it.id in postUpdate.attachmentDeleteIds!! }
        val newAttachments = postUpdate.images?.map{
            fileService.uploadImage(it)
        } ?: emptyList()
        post.images = (imagesToKeep + newAttachments).toMutableList()
        val updatedPost = postRepository.save(post)
        imagesToDelete.forEach { runCatching { fileService.deleteImage(it.id) }.onFailure { log.error(it) { it.message } } }

        return updatedPost
    }

    @Transactional
    fun delete(post: Post) {
        post.images.map{
            fileService.deleteImage(it.id)
        }
        postRepository.deleteById(post.id!!)
    }

    @Transactional
    fun addCommentToPost(post: Post, content: String, author: User): Comment {
        val newComment = Comment(
            content = content,
            author = author,
        )
        post.comments.add(newComment)
        postRepository.save(post)
        return newComment
    }

    @Transactional
    fun removeCommentFromPost(post: Post, commentId: String): Boolean {
        val comment = post.comments.find { it.id.toString() == commentId }!!

        val removed = post.comments.remove(comment)
        if (removed) {
            postRepository.save(post)
        }
        return removed
    }

    @Transactional
    fun addReplyToComment(post: Post, commentId: String, content: String, author: User): Reply {
        val comment = post.comments.find { it.id.toString() == commentId }!!
        val newReply = Reply(
            content = content,
            author = author,
        )
        comment.replies.add(newReply)
        postRepository.save(post)
        return newReply
    }

    @Transactional
    fun removeReplyFromComment(post: Post, commentId: String, replyId: String): Boolean {
        val comment = post.comments.find { it.id.toString() == commentId }!!
        val reply = comment.replies.find { it.id.toString() == replyId }!!

        comment.replies.remove(reply)
        postRepository.save(post)
        return true
    }

    @Transactional
    fun updateComment(post: Post, commentId: String, commentUpdate: CommentUpdate): Comment {
        val comment = post.comments.find { it.id.toString() == commentId }!!
        comment.content = commentUpdate.content
        comment.updatedAt = System.currentTimeMillis()

        postRepository.save(post)
        return comment
    }

    @Transactional
    fun updateReply(post: Post, commentId: String, replyId: String, replyUpdate: CommentUpdate): Reply {
        val comment = post.comments.find { it.id.toString() == commentId }!!
        val reply = comment.replies.find { it.id.toString() == replyId }!!

        reply.content = replyUpdate.content
        reply.updatedAt = System.currentTimeMillis()

        postRepository.save(post)
        return reply
    }

    @Transactional
    fun addLikeToPost(post: Post, user: User): Boolean {
        val like = post.likes.find { it.userId == user.id }
        if (like != null) {
            if (!like.isActive) {
                like.isActive = true
                like.updatedAt = System.currentTimeMillis()
                postRepository.save(post)
                return true
            }
            return false
        } else {
            post.likes.add(Like(userId = user.id!!, isActive = true, updatedAt = System.currentTimeMillis()))
            postRepository.save(post)
            return true
        }
    }

    @Transactional
    fun removeLikeFromPost(post: Post, user: User): Boolean {
        val like = post.likes.find { it.userId == user.id && it.isActive }
        if (like != null) {
            like.isActive = false
            like.updatedAt = System.currentTimeMillis()
            postRepository.save(post)
            return true
        }
        return false
    }

    @Transactional
    fun addLikeToComment(post: Post, commentId: String, user: User): Boolean {
        val comment = post.comments.find { it.id.toString() == commentId }
        if (comment != null) {
            val like = comment.likes.find { it.userId == user.id }
            if (like != null) {
                if (!like.isActive) {
                    like.isActive = true
                    like.updatedAt = System.currentTimeMillis()
                    postRepository.save(post)
                    return true
                }
                return false
            } else {
                comment.likes.add(Like(userId = user.id!!, isActive = true, updatedAt = System.currentTimeMillis()))
                postRepository.save(post)
                return true
            }
        }
        return false
    }

    @Transactional
    fun removeLikeFromComment(post: Post, commentId: String, user: User): Boolean {
        val comment = post.comments.find { it.id.toString() == commentId }
        if (comment != null) {
            val like = comment.likes.find { it.userId == user.id && it.isActive }
            if (like != null) {
                like.isActive = false
                like.updatedAt = System.currentTimeMillis()
                postRepository.save(post)
                return true
            }
        }
        return false
    }

    @Transactional
    fun addLikeToReply(post: Post, commentId: String, replyId: String, user: User): Boolean {
        val comment = post.comments.find { it.id.toString() == commentId }
        val reply = comment?.replies?.find { it.id.toString() == replyId }
        if (reply != null) {
            val like = reply.likes.find { it.userId == user.id }
            if (like != null) {
                if (!like.isActive) {
                    like.isActive = true
                    like.updatedAt = System.currentTimeMillis()
                    postRepository.save(post)
                    return true
                }
                return false
            } else {
                reply.likes.add(Like(userId = user.id!!, isActive = true, updatedAt = System.currentTimeMillis()))
                postRepository.save(post)
                return true
            }
        }
        return false
    }

    @Transactional
    fun removeLikeFromReply(post: Post, commentId: String, replyId: String, user: User): Boolean {
        val comment = post.comments.find { it.id.toString() == commentId }
        val reply = comment?.replies?.find { it.id.toString() == replyId }
        if (reply != null) {
            val like = reply.likes.find { it.userId == user.id && it.isActive }
            if (like != null) {
                like.isActive = false
                like.updatedAt = System.currentTimeMillis()
                postRepository.save(post)
                return true
            }
        }
        return false
    }

    fun addViewer(post: Post, user: User): Boolean {
        if (post.viewerIds.contains(user.id)) {
            return false
        }
        post.viewerIds.add(user.id!!)
        postRepository.save(post)
        return true
    }
}
