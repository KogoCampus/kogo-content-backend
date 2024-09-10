package com.kogo.content.endpoint.model

import com.kogo.content.validator.ValidTag
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.service.util.Transformer
import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KParameter

data class TopicDto (
    @field:NotBlank
    var topicName: String,

    var description: String,

    @ValidTag
    var tags: List<String> = emptyList(),

    var profileImage: MultipartFile? = null
)
