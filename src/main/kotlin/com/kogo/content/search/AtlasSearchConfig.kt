package com.kogo.content.search

import com.kogo.content.logging.Logger
import jakarta.annotation.PostConstruct
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
class AtlasSearchConfig @Autowired constructor(
    private val applicationContext: ApplicationContext,
    private val mongoTemplate: MongoTemplate
) {
    companion object : Logger()

    @PostConstruct
    fun initializeSearchIndexes() {
        val searchIndexBeans = applicationContext.getBeansOfType(SearchIndex::class.java)

        searchIndexBeans.values.forEach { searchIndex ->
            val indexName = searchIndex.getIndexName()
            val collectionName = searchIndex.getCollectionName()

            if (!isIndexExists(indexName, collectionName)) {
                createSearchIndex(indexName, collectionName, searchIndex.getMapping())
            }
        }
    }

    private fun isIndexExists(indexName: String, collectionName: String): Boolean {
        return try {
            val listIndexesCommand = Document("listSearchIndexes", collectionName)
            val indexes = mongoTemplate.db.runCommand(listIndexesCommand)
            val cursor = indexes.get("cursor") as? Document
            val firstBatch = cursor?.get("firstBatch") as? List<*>

            firstBatch?.any { (it as? Document)?.get("name") == indexName } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun createSearchIndex(indexName: String, collectionName: String, mapping: SearchMapping) {
        if (mongoTemplate.collectionExists(collectionName)) {
            val command = Document().apply {
                put("createSearchIndexes", collectionName)
                put("indexes", listOf(Document().apply {
                    put("name", indexName)
                    put("definition", mapping.toDocument())
                }))
            }

            try {
                mongoTemplate.db.runCommand(command)
                log.info { "Search index '$indexName' created successfully." }
            } catch (e: Exception) {
                log.info { "Failed to create search index '$indexName': ${e.message}" }
                throw e
            }
        }
        else {
            log.info { "Collection does not exist. Search index $indexName will not be created." }
        }
    }
}
