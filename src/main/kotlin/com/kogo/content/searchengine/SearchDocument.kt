package com.kogo.content.searchengine

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kogo.content.storage.entity.Comment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic

abstract class DocumentBody() {

    abstract fun toJsonNode(): JsonNode

    class PostIndexDocumentBody(val post: Post) : DocumentBody() {
        private val objectMapper = ObjectMapper()

        override fun toJsonNode(): JsonNode {
            val rootNode = objectMapper.createObjectNode()
            rootNode.put("id", post.id)
            rootNode.put("title", post.title)
            rootNode.put("content", post.content)
            rootNode.put("authorId", post.author.id)
            rootNode.put("topicId", post.topic.id)
            return rootNode
        }
    }

    class CommentIndexDocumentBody(val comment: Comment) : DocumentBody() {
        private val objectMapper = ObjectMapper()
        override fun toJsonNode(): JsonNode {
            val rootNode = objectMapper.createObjectNode()
            rootNode.put("id", comment.id)
            rootNode.put("parentId", comment.parentId)
            rootNode.put("parentType", comment.parentType.name)
            rootNode.put("content", comment.content)
            rootNode.put("authorId", comment.author.id)
            return rootNode
        }
    }

    class TopicIndexDocumentBody(val topic: Topic) : DocumentBody() {
        private val objectMapper = ObjectMapper()
        override fun toJsonNode(): JsonNode {
            val rootNode = objectMapper.createObjectNode()
            rootNode.put("id", topic.id)
            rootNode.put("topicName", topic.topicName)
            rootNode.put("description", topic.description)
            rootNode.put("ownerId", topic.owner.id)
            val tagsArrayNode = objectMapper.createArrayNode()
            topic.tags.forEach { tag ->
                tagsArrayNode.add(tag)
            }
            rootNode.set<JsonNode>("tags", tagsArrayNode)
            return rootNode
        }
    }
}
