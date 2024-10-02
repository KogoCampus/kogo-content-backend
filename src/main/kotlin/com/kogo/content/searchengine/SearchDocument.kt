package com.kogo.content.searchengine

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class Document(val documentId: String) {

    val objectMapper = ObjectMapper()
    val rootNode = objectMapper.createObjectNode()
    init {
        rootNode.put("id", documentId)
    }

    fun toJsonNode(): JsonNode = rootNode

    fun put(fieldName: String, value: String): Document {
        rootNode.put(fieldName, value)
        return this
    }

    fun put(fieldName: String, values: List<String>): Document {
        val arrayNode = objectMapper.createArrayNode()
        values.forEach { arrayNode.add(it) }
        rootNode.set<JsonNode>(fieldName, arrayNode)
        return this
    }

    fun put(fieldName: String, value: Long): Document {
        rootNode.put(fieldName, value)
        return this
    }

}
