package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidTag
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.service.util.Transformer
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KParameter

data class TopicDto (
    @field:NotBlank
    var topicName: String,

    var description: String,

    @ValidTag
    @ArraySchema(schema = Schema(description = "list of tags to attach to the topic. Valid tag must not contain any special characters.", type = "String"))
    var tags: List<String>? = emptyList(),

    var profileImage: MultipartFile? = null
)
