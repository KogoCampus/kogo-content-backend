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

    companion object {
        fun createPostIndexDocument(post: Post): Document {
            return Document(post.id!!).apply {
                put("title", post.title)
                put("content", post.content)
                put("authorId", post.author.id!!)
                put("topicId", post.topic.id!!)
            }
        }

        fun createCommentIndexDocument(comment: Comment): Document {
            return Document(comment.id!!).apply {
                put("parentId", comment.parentId)
                put("parentType", comment.parentType.name)
                put("content", comment.content)
                put("authorId", comment.author.id!!)
            }
        }

        fun createTopicIndexDocument(topic: Topic): Document {
            return Document(topic.id!!).apply {
                put("topicName", topic.topicName)
                put("description", topic.description)
                put("ownerId", topic.owner.id!!)
                put("tags", topic.tags)
            }
        }
    }
}
