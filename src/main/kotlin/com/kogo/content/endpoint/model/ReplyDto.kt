package com.kogo.content.endpoint.model

import com.kogo.content.storage.model.entity.User
import jakarta.validation.constraints.NotBlank

data class ReplyDto (
    var content: String,

    var mentionUserId: String? = null,
)
