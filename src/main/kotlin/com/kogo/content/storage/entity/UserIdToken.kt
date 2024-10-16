package com.kogo.content.storage.entity

import java.util.Base64

data class UserIdToken(val username: String, val email: String) {

    // Function to retrieve the encoded token
    private fun getEncodedToken(): String {
        return Base64.getEncoder().encodeToString("$username:$email".toByteArray())
    }

    companion object {
        fun parse(idToken: String): UserIdToken {
            val decodedString = String(Base64.getDecoder().decode(idToken))
            val username = decodedString.substringBefore(":")
            val email = decodedString.substringAfter(":")
            return UserIdToken(username, email)
        }
    }

    // Override toString to return the encoded token
    override fun toString(): String {
        return getEncodedToken()
    }
}
