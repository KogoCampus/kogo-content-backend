package com.kogo.content.endpoint.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.web.multipart.MultipartFile

data class PostUpdate (
    var title: String? = null,

    var content: String? = null,

    @ArraySchema(schema = Schema(description = "list of attachment ids to delete from the post", type = "String"))
    var attachmentDelete: List<String>? = listOf(),

    @ArraySchema(schema = Schema(description = "list of image files to add to the post", type = "File"))
    var images: List<MultipartFile>? = listOf(),

    @ArraySchema(schema = Schema(description = "list of video files to add to the post", type = "File"))
    var videos: List<MultipartFile>? = listOf(),
)
