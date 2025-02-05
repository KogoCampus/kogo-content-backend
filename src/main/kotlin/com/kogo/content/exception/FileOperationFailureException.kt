package com.kogo.content.exception

class FileOperationFailureException(
    val operation: FileOperationFailure,
    val contentType: String?,
    val details: String
) : RuntimeException("Failed to $operation files: $details - $contentType")

enum class FileOperationFailure{
    UPLOAD,
    DELETE,
    CONNECT,
    KEEP
}
