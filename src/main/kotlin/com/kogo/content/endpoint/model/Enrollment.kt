package com.kogo.content.endpoint.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class Enrollment (

    @field:NotBlank
    var schoolKey: String,

    @ArraySchema(schema = Schema(description = "list of course codes for course groups to join", type = "String"))
    var base64CourseCodes: List<String> = emptyList(),
)
