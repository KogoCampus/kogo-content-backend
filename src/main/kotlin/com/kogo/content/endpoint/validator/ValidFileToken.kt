package com.kogo.content.endpoint.validator

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.reflect.KClass

@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = [FileTokenValidator::class, FileTokenListValidator::class])
annotation class ValidFileToken(
    val acceptedMediaTypes: Array<String>,
    val message: String = "Invalid or expired file token",
    val groups: Array<KClass<Any>> = [],
    val payload: Array<KClass<Payload>> = []
)
