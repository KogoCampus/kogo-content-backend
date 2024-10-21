package com.kogo.content.exception

//data class UserIsNotOwnerException(
//    val resourceName: String,
//    val resourceId: String,
//    val details: String? = ""
//) : RuntimeException(details) {
//
//    companion object {
//        inline fun <reified T> of(resourceId: String): UserIsNotOwnerException {
//            val resourceName = T::class.simpleName
//            return UserIsNotOwnerException(
//                resourceName = resourceName!!,
//                resourceId = resourceId,
//                details = "$resourceName with id: $resourceId cannot be accessed because you are not the owner"
//            )
//        }
//    }
//}
