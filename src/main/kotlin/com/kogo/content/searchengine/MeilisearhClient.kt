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

    override fun deleteDocument(index: SearchIndex, entityId: String){
        //SEARCH
        val documentId = searchDocument(index, entityId)
        //DELETE
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer ${getAuthToken()}")

        val deletePath = "$meilisearchHost/indexes/${index.indexId}/documents/$documentId"
        val deleteEntity = HttpEntity<String>(headers)
        val deleteResponse: ResponseEntity<String> = restTemplate.exchange(deletePath, HttpMethod.DELETE, deleteEntity, String::class.java)

        if (!deleteResponse.statusCode.is2xxSuccessful) {
            throw RuntimeException("Failed to delete document with ID: $documentId - ${deleteResponse.statusCode}")
        }

        println("Successfully deleted document with entity ID: $entityId from index: ${index.indexId}")
    }
    override fun updateDocument(index: SearchIndex, updatedDocument: Document){
        val jsonNode = updatedDocument.toJsonNode()
        //UPDATE
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer ${getAuthToken()}")
        headers.set("Content-Type", "application/json")

        val updatePath = "$meilisearchHost/indexes/${index.indexId}/documents"
        val entity = HttpEntity(jsonNode, headers)
        val updateResponse: ResponseEntity<String> = restTemplate.exchange(updatePath, HttpMethod.PUT, entity, String::class.java)
        if (!updateResponse.statusCode.is2xxSuccessful) {
            throw RuntimeException("Failed to update document ${updateResponse.statusCode}")
        }

        println("Successfully updated document from index: ${index.indexId} - ${updateResponse.body}")
    }

    override fun searchDocument(index: SearchIndex, entityId: String): String{
        val searchPath = "$meilisearchHost/indexes/${index.indexId}/search"
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer ${getAuthToken()}")
        val searchBody = mapOf(
            "q" to "",
            "filter" to "id = '$entityId'"
        )

        val searchEntity = HttpEntity(searchBody, headers)
        val searchResponse: ResponseEntity<String> = restTemplate.exchange(searchPath, HttpMethod.POST, searchEntity, String::class.java)
        if (!searchResponse.statusCode.is2xxSuccessful) {
            throw RuntimeException("Failed to search documents for entity ID: $entityId - ${searchResponse.statusCode}")
        }

        val responseNode = ObjectMapper().readTree(searchResponse.body)
        val hitsNode = responseNode.get("hits")

        if (hitsNode.isEmpty) {
            throw RuntimeException("No document found with entity ID: $entityId")
        }
        return hitsNode[0].get("id").asText()
    }

    override fun searchDocuments(indexes: List<SearchIndex>, queryOptions: String?): Map<SearchIndex, List<Document>> {
        val path = "$meilisearchHost/multi-search"
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer ${getAuthToken()}")

        val objectMapper = ObjectMapper()
        val searchQueries = indexes.map { index ->
            objectMapper.createObjectNode().apply {
                put("indexUid", index.indexId)
                if (queryOptions != null) {
                    put("q", queryOptions)
                }
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
