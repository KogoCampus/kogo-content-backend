package com.kogo.content.storage.repository

interface PostRepositoryCustom {
    fun updateLikes(postId: String, alreadyLiked: Boolean)
}
