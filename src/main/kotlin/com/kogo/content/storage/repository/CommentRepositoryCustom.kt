package com.kogo.content.storage.repository

interface CommentRepositoryCustom {
    fun addLike(commentId: String)
    fun removeLike(commentId: String)
}
