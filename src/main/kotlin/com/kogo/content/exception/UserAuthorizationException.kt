package com.kogo.content.exception

open class ActionDeniedException(
    val resourceName: String,
    val resourceId: String,
    val details: String
) : RuntimeException(details)

class UserIsNotOwnerException(
    resourceName: String,
    resourceId: String,
    details: String = "You are not the owner of $resourceName with id: $resourceId."
) : ActionDeniedException(resourceName, resourceId, details) {
    companion object {
        inline fun <reified T> of(resourceId: String): UserIsNotOwnerException {
            val resourceName = T::class.simpleName ?: "Resource"
            return UserIsNotOwnerException(resourceName, resourceId)
        }
    }
}

class UserIsNotMemberException(
    resourceName: String,
    resourceId: String,
    details: String = "You are not a member of $resourceName with id: $resourceId."
) : ActionDeniedException(resourceName, resourceId, details) {
    companion object {
        inline fun <reified T> of(resourceId: String): UserIsNotMemberException {
            val resourceName = T::class.simpleName ?: "Resource"
            return UserIsNotMemberException(resourceName, resourceId)
        }
    }
}
