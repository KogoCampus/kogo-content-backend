package com.kogo.content.endpoint.model

import jakarta.validation.constraints.NotBlank

data class CommentDto (
    @field:NotBlank
    var content: String,
)
