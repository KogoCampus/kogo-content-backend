package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidTag
import com.kogo.content.validator.ValidFile
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

data class TopicDto (
    @field:NotBlank
    var topicName: String,

    var description: String,

    @ArraySchema(schema = Schema(description = "list of tags to attach to the topic. Valid tag must not contain any special characters.", type = "String"))
    @field:ValidTag
    var tags: List<String>? = emptyList(),

    @field:ValidFile(
        sizeMax = 128000000, // 128MB
        acceptedMediaTypes = [MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE],
        message = "An image must have either 'image/png' or 'image/jpeg' media type and maximum size 128MB")
    var profileImage: MultipartFile? = null
)
