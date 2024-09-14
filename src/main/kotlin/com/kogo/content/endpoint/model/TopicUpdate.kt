package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidTag
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.service.util.Transformer
import com.kogo.content.validator.ValidFile
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KParameter

data class TopicUpdate (
    var topicName: String? = null,

    var description: String? = null,

    @ArraySchema(schema = Schema(description = "list of tags to attach to the topic. Beware that previously attached tags will be removed.", type = "String"))
    @field:ValidTag
    var tags: List<String>? = emptyList(),

    @field:ValidFile(
        sizeMax = 128000000, // 128MB
        acceptedMediaTypes = [MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE],
        message = "An image must have either 'image/png' or 'image/jpeg' media type and maximum size 128MB")
    var profileImage: MultipartFile? = null
)
