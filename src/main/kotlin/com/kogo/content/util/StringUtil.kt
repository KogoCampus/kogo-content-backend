package com.kogo.content.util

fun convertTo12BytesHexString(key: String): String {
    // Create a deterministic hex string
    val md5Bytes = java.security.MessageDigest.getInstance("MD5")
        .digest(key.toByteArray())

    val hexString = md5Bytes.take(12)
        .joinToString("") { "%02x".format(it) }

    return "60000000$hexString".take(24)
}

fun generateRandomCode(length: Int): String {
    require(length > 0) { "Length must be greater than 0" }
    val allowedChars = ('A'..'Z') + ('0'..'9')  // Uppercase letters and numbers
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}
