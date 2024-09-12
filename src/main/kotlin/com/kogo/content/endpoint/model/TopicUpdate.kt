package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidTag
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.service.util.Transformer
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KParameter

data class TopicUpdate (
    var topicName: String? = null,

    var description: String? = null,

    @ValidTag
    @ArraySchema(schema = Schema(description = "list of tags to attach to the topic. Beware that previously attached tags will be removed.", type = "String"))
    var tags: List<String>? = emptyList(),

    var profileImage: MultipartFile? = null
)
