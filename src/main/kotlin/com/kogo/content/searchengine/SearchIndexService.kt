package com.kogo.content.searchengine

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
interface SearchIndexService {
    fun addDocument(index: SearchIndex, document: Document)
//    fun deleteDocument()
//    fun updateDocument()
    fun searchDocuments(indexes: List<SearchIndex>, queryOptions: String): Map<SearchIndex, List<Document>>
}
