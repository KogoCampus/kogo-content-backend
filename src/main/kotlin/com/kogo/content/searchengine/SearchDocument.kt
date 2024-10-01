package com.kogo.content.searchengine

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic

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

}
