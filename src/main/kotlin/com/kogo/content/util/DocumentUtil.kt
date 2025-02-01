package com.kogo.content.util

fun convertTo12BytesHexString(key: String): String {
    // Create a deterministic hex string
    val md5Bytes = java.security.MessageDigest.getInstance("MD5")
        .digest(key.toByteArray())

    val hexString = md5Bytes.take(12)
        .joinToString("") { "%02x".format(it) }

    return "60000000$hexString".take(24)
}
