package com.kogo.content.searchengine

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
interface SearchIndexService {
    fun addDocument(index: SearchIndex, document: DocumentBody)
//    fun deleteDocument()
//    fun updateDocument()
    fun searchPosts(indexes: List<SearchIndex>, queryOptions: String): List<String>
}
