package com.kogo.content.endpoint.model

import com.kogo.content.endpoint.validator.ValidFile
import com.kogo.content.endpoint.validator.ValidFileToken
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

data class PostDto (
    @field:NotBlank
    var title: String,

    @field:NotBlank
    var content: String,

    @ArraySchema(schema = Schema(description = "List of file tokens to validate and persist", type = "String"))
    @field:ValidFileToken(
        acceptedMediaTypes = [MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE],
        message = "Invalid or expired file token"
    )
    var fileTokens: List<String>? = listOf(),
)
