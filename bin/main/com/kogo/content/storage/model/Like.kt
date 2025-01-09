package com.kogo.content.storage.model

import java.time.Instant

data class Like(
    var userId: String,
    var isActive: Boolean,
    var updatedAt: Long = System.currentTimeMillis(),
    var createdAt: Long = System.currentTimeMillis()
)
