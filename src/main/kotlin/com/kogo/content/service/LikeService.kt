package com.kogo.content.service

import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.entity.Like
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.repository.*
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LikeService (
    private val likeRepository: LikeRepository,
    private val postRepository: PostRepository,
) {
    @Transactional
    fun create(parentId: String, user: UserDetails) {
        val userId = user.id!!
        val like = Like(
            userId = userId,
            parentId = parentId,
        )
        likeRepository.save(like)
        postRepository.updateLikes(parentId, false)
    }

    @Transactional
    fun delete(parentId: String, user: UserDetails) {
        val userId = user.id!!
        val like = likeRepository.findByUserIdAndParentId(userId, parentId)
        likeRepository.deleteById(like!!.id!!)
        postRepository.updateLikes(parentId, true)
    }

    fun findByUserIdAndParentId(userId: String, parentId: String): Like? =
        likeRepository.findByUserIdAndParentId(userId, parentId)
}
