package com.kogo.content.service.pagination

import java.util.Base64

data class PageToken(
    val pageLastResourceId: String? = null,
    val cursorAttributes: Map<String, String> = emptyMap()
) {
    companion object {
        fun create(pageLastResourceId: String? = null, cursorAttributes: Map<String, String> = emptyMap()): PageToken {
            return PageToken(pageLastResourceId, cursorAttributes)
        }

        fun fromString(token: String): PageToken {
            val decodedToken = String(Base64.getDecoder().decode(token))
            val parts = decodedToken.split(";")

            // Handle optional pageLastResourceId
            val pageLastResourceId = parts.getOrNull(0)?.takeIf { it.isNotEmpty() }
            val cursorAttributes = parts
                .drop(1) // Skip the pageLastResourceId part
                .map { it.split("=", limit = 2) }
                .associate { it[0] to it.getOrElse(1) { "" } }

            return PageToken(pageLastResourceId, cursorAttributes)
        }
    }

    override fun toString(): String {
        val idPart = pageLastResourceId ?: ""
        val attributesPart = cursorAttributes.entries.joinToString(";") { "${it.key}=${it.value}" }
        val tokenString = if (attributesPart.isEmpty()) idPart else "$idPart;$attributesPart"
        return Base64.getEncoder().encodeToString(tokenString.toByteArray())
    }

    fun nextPageToken(nextPageLastResourceId: String, newAttributes: Map<String, String> = cursorAttributes): PageToken {
        return PageToken(nextPageLastResourceId, newAttributes)
    }

    fun getAttribute(key: String): String? = cursorAttributes[key]
}
