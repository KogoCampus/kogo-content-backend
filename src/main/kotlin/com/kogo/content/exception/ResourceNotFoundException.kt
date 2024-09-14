package com.kogo.content.exception

data class ResourceNotFoundException (
    val resourceName: String,
    val resourceId: String,
    val details: String?) : RuntimeException(details) {
        companion object {
            inline fun<reified T> of(resourceId: String): ResourceNotFoundException {
                val resourceName = T::class.simpleName
                return ResourceNotFoundException(
                    resourceName = resourceName!!,
                    resourceId = resourceId,
                    details = "$resourceName not found for id: $resourceId"
                )
            }
        }
    }
