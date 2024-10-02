package com.kogo.content.searchengine

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.Instant

@Component
interface SearchIndexService {
    fun addDocument(index: SearchIndex, document: Document)
    fun deleteDocument(index: SearchIndex, documentId: String)
    fun updateDocument(index: SearchIndex, updatedDocument: Document)
    fun searchDocument(index: SearchIndex, entityId: String): String
    fun searchDocuments(indexes: List<SearchIndex>, queryOptions: String?, pageTimestamp: Long?, limit: Int?): Map<SearchIndex, List<Document>>
}
