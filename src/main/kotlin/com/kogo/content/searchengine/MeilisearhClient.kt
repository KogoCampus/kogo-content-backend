package com.kogo.content.searchengine

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class MeilisearhClient: SearchIndexService {
    @Value("\${meilisearch.host}")
    lateinit var meilisearchHost: String

    @Value("\${meilisearch.masterkey}")
    lateinit var masterKey: String

    @Value("\${meilisearch.apikey:}")
    lateinit var apiKey: String

    private val restTemplate = RestTemplate()

    private fun getAuthToken(): String {
        return if (apiKey.isNotEmpty()) apiKey else masterKey
    }

    override fun addDocument(index: SearchIndex, document: Document){
        val jsonNode = document.toJsonNode()

        val path = "$meilisearchHost/indexes/${index.indexId}/documents"

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer ${getAuthToken()}")

        val entity = HttpEntity(jsonNode, headers)
        val documentResponse: ResponseEntity<String> = restTemplate.exchange(path, HttpMethod.POST, entity, String::class.java)
        if (documentResponse.statusCode.is2xxSuccessful) {
            println("Successfully added document: $documentResponse.body")
        } else {
            throw RuntimeException("Meilisearch document request failed: ${documentResponse.statusCode}")
        }
    }

    override fun searchDocuments(indexes: List<SearchIndex>, queryOptions: String): Map<SearchIndex, List<Document>> {
        val path = "$meilisearchHost/multi-search"

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer ${getAuthToken()}")

        val objectMapper = ObjectMapper()
        val searchQueries = indexes.map { index ->
            objectMapper.createObjectNode().apply {
                put("indexUid", index.indexId)
                put("q", queryOptions)
                if(index.indexId == "comments"){
                    put("filter", "parentType= 'POST'")
                }
            }
        }

        val bodyNode = objectMapper.createObjectNode().apply {
            set<ArrayNode>("queries", objectMapper.valueToTree(searchQueries))
        }

        val entity = HttpEntity(bodyNode, headers)

        val multiSearchResponse: ResponseEntity<String> = restTemplate.exchange(path, HttpMethod.POST, entity, String::class.java)
        if (!multiSearchResponse.statusCode.is2xxSuccessful) {
            throw RuntimeException("Meilisearch document request failed: ${multiSearchResponse.statusCode}")
        }

        val responseNode = objectMapper.readTree(multiSearchResponse.body)
        val resultsNode = responseNode.get("results")

        val resultMap = mutableMapOf<SearchIndex, MutableList<Document>>()

        indexes.forEach { index ->
            val indexResults = resultsNode.find { it.get("indexUid").asText() == index.indexId }?.get("hits")
            val documents = mutableListOf<Document>()

            indexResults?.forEach { hit ->
                val documentId = hit.get("id").asText()
                val document = when (index.indexId) {
                    "posts" -> {
                        Document(documentId).apply {
                            put("title", hit.get("title").asText())
                            put("content", hit.get("content").asText())
                            put("authorId", hit.get("authorId").asText())
                            put("topicId", hit.get("topicId").asText())
                        }
                    }
                    "comments" -> {
                        Document(documentId).apply {
                            put("parentId", hit.get("parentId").asText())
                            put("parentType", hit.get("parentType").asText())
                            put("content", hit.get("content").asText())
                            put("authorId", hit.get("authorId").asText())
                        }
                    }
                    "topics" -> {
                        Document(documentId).apply {
                            put("topicName", hit.get("topicName").asText())
                            put("description", hit.get("description").asText())
                            put("ownerId", hit.get("ownerId").asText())
                            put("tags", hit.get("tags").map { it.asText() })
                        }
                    }
                    else -> throw IllegalArgumentException("Unknown index: ${index.indexId}")
                }
                documents.add(document)
            }
            resultMap[index] = documents
        }
        return resultMap
    }
}
