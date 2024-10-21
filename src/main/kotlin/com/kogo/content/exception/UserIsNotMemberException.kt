package com.kogo.content.exception

//data class UserIsNotMemberException(
//    val resourceName: String,
//    val resourceId: String,
//    val details: String? = ""
//) : RuntimeException(details) {
//
//    companion object {
//        inline fun <reified T> of(resourceId: String): UserIsNotMemberException {
//            val resourceName = T::class.simpleName
//            return UserIsNotMemberException(
//                resourceName = resourceName!!,
//                resourceId = resourceId,
//                details = "$resourceName with id: $resourceId cannot be accessed because you are not a member"
//            )
//        }
//    }
//}
