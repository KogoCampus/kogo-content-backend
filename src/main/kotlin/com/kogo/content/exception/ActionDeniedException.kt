package com.kogo.content.exception

import com.kogo.content.endpoint.common.ErrorCode

abstract class ActionDeniedException(
    val resourceName: String,
    val resourceId: String,
    val details: String
) : RuntimeException(details) {
    abstract fun errorCode(): ErrorCode
}

class UserIsNotOwnerException(
    resourceName: String,
    resourceId: String,
    details: String = "You are not the owner of $resourceName with id: $resourceId."
) : ActionDeniedException(resourceName, resourceId, details) {
    override fun errorCode() = ErrorCode.USER_IS_NOT_OWNER
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
    override fun errorCode() = ErrorCode.USER_IS_NOT_MEMBER
    companion object {
        inline fun <reified T> of(resourceId: String): UserIsNotMemberException {
            val resourceName = T::class.simpleName ?: "Resource"
            return UserIsNotMemberException(resourceName, resourceId)
        }
    }
}
