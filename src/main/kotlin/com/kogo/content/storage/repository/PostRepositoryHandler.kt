package com.kogo.content.storage.repository

interface PostRepositoryHandler {
    fun addLike(postId: String)
    fun removeLike(postId: String)
    fun addView(postId: String)
}
