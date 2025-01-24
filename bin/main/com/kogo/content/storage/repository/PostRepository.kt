package com.kogo.content.storage.repository

import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.Like
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate

interface PostRepository: MongoRepository<Post, String>, PostRepositoryCustom {
    fun findAllByAuthorId(authorId: String): List<Post>
}

interface PostRepositoryCustom {
    fun addLikeToPost(post: Post, userId: String): Boolean
    fun removeLikeFromPost(post: Post, userId: String): Boolean
    fun addLikeToComment(post: Post, commentId: String, userId: String): Boolean
    fun removeLikeFromComment(post: Post, commentId: String, userId: String): Boolean
    fun addLikeToReply(post: Post, commentId: String, replyId: String, userId: String): Boolean
    fun removeLikeFromReply(post: Post, commentId: String, replyId: String, userId: String): Boolean
}

class PostRepositoryCustomImpl : PostRepositoryCustom {
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    private fun handleLikeOperation(
        post: Post,
        likes: MutableList<Like>,
        userId: String,
        isAdd: Boolean
    ): Boolean {
        if (isAdd) {
            val like = likes.find { it.userId == userId }
            if (like != null) {
                if (!like.isActive) {
                    like.isActive = true
                    like.updatedAt = System.currentTimeMillis()
                    mongoTemplate.save(post)
                    return true
                }
                return false
            } else {
                likes.add(Like(userId = userId, isActive = true))
                mongoTemplate.save(post)
                return true
            }
        } else {
            val like = likes.find { it.userId == userId && it.isActive }
            if (like != null) {
                like.isActive = false
                like.updatedAt = System.currentTimeMillis()
                mongoTemplate.save(post)
                return true
            }
            return false
        }
    }

    override fun addLikeToPost(post: Post, userId: String): Boolean {
        return handleLikeOperation(post, post.likes, userId, true)
    }

    override fun removeLikeFromPost(post: Post, userId: String): Boolean {
        return handleLikeOperation(post, post.likes, userId, false)
    }

    override fun addLikeToComment(post: Post, commentId: String, userId: String): Boolean {
        val comment = post.comments.find { it.id == commentId } ?: return false
        return handleLikeOperation(post, comment.likes, userId, true)
    }

    override fun removeLikeFromComment(post: Post, commentId: String, userId: String): Boolean {
        val comment = post.comments.find { it.id == commentId } ?: return false
        return handleLikeOperation(post, comment.likes, userId, false)
    }

    override fun addLikeToReply(post: Post, commentId: String, replyId: String, userId: String): Boolean {
        val comment = post.comments.find { it.id == commentId } ?: return false
        val reply = comment.replies.find { it.id == replyId } ?: return false
        return handleLikeOperation(post, reply.likes, userId, true)
    }

    override fun removeLikeFromReply(post: Post, commentId: String, replyId: String, userId: String): Boolean {
        val comment = post.comments.find { it.id == commentId } ?: return false
        val reply = comment.replies.find { it.id == replyId } ?: return false
        return handleLikeOperation(post, reply.likes, userId, false)
    }
}
