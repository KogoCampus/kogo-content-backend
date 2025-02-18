package com.kogo.content.endpoint.model

data class ReplyUpdate (
    var content: String,

    var mentionUserId: String? = null,
)
