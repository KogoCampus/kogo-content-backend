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
@Constraint(validatedBy = [TagValidator::class, TagListValidator::class])
annotation class ValidTag (
    val message: String = "No special characters allowed.",
    val groups: Array<KClass<Any>> = [],
    val payload: Array<KClass<Payload>> = []
)
