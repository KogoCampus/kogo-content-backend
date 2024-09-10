package com.kogo.content.endpoint.model

import org.springframework.web.multipart.MultipartFile

data class PostUpdate (
    var title: String? = null,

    var content: String? = null,

    var attachmentDelete: List<String> = listOf(),

    var images: List<MultipartFile> = listOf(),

    var videos: List<MultipartFile> = listOf(),
)
