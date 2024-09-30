package com.kogo.content.searchengine

import org.springframework.beans.factory.annotation.Value
import com.fasterxml.jackson.databind.JsonNode
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

    private fun sendToMeilisearch(path: String, method: HttpMethod, body: JsonNode): List<String> {
        val url = "$meilisearchHost$path"

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer ${getAuthToken()}")

        val entity = HttpEntity(body, headers)

        return if (body.has("queries")) {
            val multiSearchResponse: ResponseEntity<String> = restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java)
            if (multiSearchResponse.statusCode.is2xxSuccessful) {
                val objectMapper = ObjectMapper()
                val responseNode = objectMapper.readTree(multiSearchResponse.body)

                val postResults = responseNode.get("results").find { it.get("indexUid").asText() == "posts" }
                val commentResults = responseNode.get("results").find { it.get("indexUid").asText() == "comments" }

                val postIdsFromComments = mutableListOf<String>()
                commentResults?.get("hits")?.forEach { hit ->
                    hit.get("parentId")?.asText()?.let { parentId ->
                        postIdsFromComments.add(parentId)
                    }
                }

                val postIdsFromPosts = mutableListOf<String>()
                postResults?.get("hits")?.forEach { hit ->
                    hit.get("id")?.asText()?.let { postId ->
                        postIdsFromPosts.add(postId)
                    }
                }

                val finalPosts = (postIdsFromComments + postIdsFromPosts).distinct()
                println("Searched posts: $finalPosts")
                finalPosts
            } else {
                throw RuntimeException("Meilisearch multi-search request failed: ${multiSearchResponse.statusCode}")
            }
        } else {
            // Add or update document
            val documentResponse: ResponseEntity<String> = restTemplate.exchange(url, method, entity, String::class.java)
            if (documentResponse.statusCode.is2xxSuccessful) {
                println("Successfully added document: $documentResponse.body")
                emptyList()
            } else {
                throw RuntimeException("Meilisearch document request failed: ${documentResponse.statusCode}")
            }
        }
    }

    override fun addDocument(index: SearchIndex, document: DocumentBody){
        val jsonNode = document.toJsonNode()

        val path = "/indexes/${index.indexId}/documents"
        sendToMeilisearch(path, HttpMethod.POST, jsonNode)
    }

    override fun searchPosts(indexes: List<SearchIndex>, queryOptions: String): List<String> {
        val path = "/multi-search"

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

        val postIds = sendToMeilisearch(path, HttpMethod.POST, bodyNode)
        return postIds
    }
}
