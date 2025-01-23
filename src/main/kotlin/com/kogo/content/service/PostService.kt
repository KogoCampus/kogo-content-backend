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
import com.kogo.content.storage.model.Notification
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
    private val fileService: FileUploaderService,
    private val pushNotificationService: PushNotificationService
) : BaseEntityService<Post, String>(Post::class, postRepository) {
    companion object : Logger()

    fun findCommentOrThrow(postId: String, commentId: String): Comment {
        val post = findOrThrow(postId)
        return post.comments.find { it.id == commentId }
            ?: throw ResourceNotFoundException.of<Comment>(commentId)
    }

    fun findReplyOrThrow(postId: String, commentId: String, replyId: String): Reply {
        val comment = findCommentOrThrow(postId, commentId)
        return comment.replies.find { it.id == replyId }
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

        // First, lookup the group details
        preAggregationOperations.add(
            Aggregation.lookup("group", "group", "_id", "groupDetails")
        )

        // Unwind the groupDetails array (since lookup returns an array)
        preAggregationOperations.add(
            Aggregation.unwind("groupDetails")
        )

        // Add match operation to filter groups
        val matchOperation = Aggregation.match(
            Criteria().orOperator(
                Criteria.where("groupDetails.isSchoolGroup").`is`(false),
                Criteria.where("group").`is`(ObjectId(user.schoolInfo.schoolGroupId))
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

        savedPost.group.followers.forEach {
            if (!it.follower.id.equals(author.id)) {
                pushNotificationService.createPushNotification(
                    Notification(
                        recipient = it.follower,
                        sender = null,
                        title = "A new post in ${savedPost.group.groupName} group!",
                        body = savedPost.title.take(50) + if (savedPost.title.length > 50) "..." else "",
                    ),
                    PushNotificationService.linkToPost(savedPost.id!!)
                )
            }
        }

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

        if (!post.author.id.equals(author.id)) {
            pushNotificationService.createPushNotification(
                Notification(
                    recipient = post.author,
                    sender = author,
                    title = newComment.content.take(50) + if (newComment.content.length > 50) "..." else "",
                    body = "${author.username} commented on your post",
                ),
                PushNotificationService.linkToComment(post.id!!, newComment.id)
            )
        }
        return newComment
    }

    @Transactional
    fun removeCommentFromPost(post: Post, commentId: String): Boolean {
        val comment = post.comments.find { it.id == commentId }!!

        val removed = post.comments.remove(comment)
        if (removed) {
            postRepository.save(post)
        }
        return removed
    }

    @Transactional
    fun addReplyToComment(post: Post, commentId: String, content: String, author: User): Reply {
        val comment = post.comments.find { it.id == commentId }!!
        val newReply = Reply(
            content = content,
            author = author,
        )
        comment.replies.add(newReply)
        postRepository.save(post)

        if (!comment.author.id.equals(author.id)) {
            pushNotificationService.createPushNotification(
                Notification(
                    recipient = post.author,
                    sender = author,
                    title = newReply.content.take(50) + if (newReply.content.length > 50) "..." else "",
                    body = "${author.username} replied to your comment",
                ),
                PushNotificationService.linkToComment(post.id!!, newReply.id)
            )
        }
        return newReply
    }

    @Transactional
    fun removeReplyFromComment(post: Post, commentId: String, replyId: String): Boolean {
        val comment = post.comments.find { it.id == commentId }!!
        val reply = comment.replies.find { it.id == replyId }!!

        comment.replies.remove(reply)
        postRepository.save(post)
        return true
    }

    @Transactional
    fun updateComment(post: Post, commentId: String, commentUpdate: CommentUpdate): Comment {
        val comment = post.comments.find { it.id == commentId }!!
        comment.content = commentUpdate.content
        comment.updatedAt = System.currentTimeMillis()

        postRepository.save(post)
        return comment
    }

    @Transactional
    fun updateReply(post: Post, commentId: String, replyId: String, replyUpdate: CommentUpdate): Reply {
        val comment = post.comments.find { it.id == commentId }!!
        val reply = comment.replies.find { it.id == replyId }!!

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

            val wasPreviouslyLiked = post.likes.any { it.userId == user.id }
            val isNotAuthor = !post.author.id.equals(user.id)

            if (!wasPreviouslyLiked && isNotAuthor) {
                pushNotificationService.createPushNotification(
                    Notification(
                        recipient = post.author,
                        sender = user,
                        title = post.title.take(50) + if (post.content.length > 50) "..." else "",
                        body = "${user.username} liked your post",
                    ),
                    PushNotificationService.linkToPost(post.id!!)
                )
            }
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

                val wasPreviouslyLiked = comment.likes.any { it.userId == user.id }
                val isNotAuthor = !comment.author.id.equals(user.id)

                if (!wasPreviouslyLiked && isNotAuthor) {
                    pushNotificationService.createPushNotification(
                        Notification(
                            recipient = comment.author,
                            sender = user,
                            title = comment.content.take(50) + if (post.content.length > 50) "..." else "",
                            body = "${user.username} liked your comment",
                        ),
                        PushNotificationService.linkToComment(post.id!!, commentId)
                    )
                }
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

                val wasPreviouslyLiked = reply.likes.any { it.userId == user.id }
                val isNotAuthor = !reply.author.id.equals(user.id)

                if (!wasPreviouslyLiked && isNotAuthor) {
                    pushNotificationService.createPushNotification(
                        Notification(
                            recipient = reply.author,
                            sender = user,
                            title = reply.content.take(50) + if (post.content.length > 50) "..." else "",
                            body = "${user.username} liked your comment",
                        ),
                        PushNotificationService.linkToReply(post.id!!, commentId, replyId)
                    )
                }
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
