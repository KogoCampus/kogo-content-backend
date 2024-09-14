package com.kogo.content.validator

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.annotation.Retention
import kotlin.annotation.Target
import kotlin.annotation.AnnotationTarget.*
import kotlin.annotation.AnnotationRetention.*
import kotlin.reflect.KClass

@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = [FileValidator::class, FileListValidator::class])
annotation class ValidFile (
    val sizeMax: Int = 0,
    val sizeMin: Int = 0,
    val acceptedMediaTypes: Array<String>,
    val message: String,
    val groups: Array<KClass<Any>> = [],
    val payload: Array<KClass<Payload>> = []
)
