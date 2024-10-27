package com.kogo.content.service.pagination

import java.time.Instant
import java.util.Base64

data class PageToken(
    val pageLastResourceId: String? = null,
    val initialRequestTimestamp: Instant = Instant.now(),
) {
    companion object {
        fun fromString(token: String): PageToken {
            val decodedToken = String(Base64.getDecoder().decode(token))
            val parts = decodedToken.split(":", limit = 2)
            require(parts.size == 2) { "Invalid token format" }

            val resourceId = parts[0].ifEmpty { null } // Convert empty string to null for first page
            val timestamp = Instant.parse(parts[1])

            return PageToken(resourceId, timestamp)
        }
    }

    /**
     * Formats the token as a base64 encoded string.
     */
    override fun toString(): String {
        val resourcePart = pageLastResourceId ?: "" // Use empty string to represent null in token format
        val tokenString = "$resourcePart:$initialRequestTimestamp"
        return Base64.getEncoder().encodeToString(tokenString.toByteArray())
    }

    fun nextPageToken(nextPageLastResourceId: String): PageToken = PageToken(nextPageLastResourceId, initialRequestTimestamp)
}
