package com.kogo.content.exception

data class InvalidFieldException(
    val field: String,
    val entityName: String,
    val operation: String
) : RuntimeException("Invalid field '$field' for $operation in entity $entityName") 