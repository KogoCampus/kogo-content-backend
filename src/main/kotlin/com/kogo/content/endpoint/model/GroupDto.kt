package com.kogo.content.endpoint.model

import com.kogo.content.endpoint.validator.ValidTag
import com.kogo.content.endpoint.validator.ValidFile
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile

data class GroupDto (
    @field:NotBlank
    var groupName: String,

    var description: String,

    @ArraySchema(schema = Schema(description = "list of tags to attach to the group. Valid tag must not contain any special characters.", type = "String"))
    @field:ValidTag
    var tags: List<String> = emptyList(),

    @field:ValidFile(
        sizeMax = 12000000, // 12MB
        acceptedMediaTypes = [MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE],
        message = "An image must have either 'image/png' or 'image/jpeg' media type and maximum size 12MB")
    var profileImage: MultipartFile? = null
)
