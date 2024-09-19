package com.kogo.content.storage.repository

interface PostRepositoryCustom {
    fun addLike(postId: String)
    fun removeLike(postId: String)
}
