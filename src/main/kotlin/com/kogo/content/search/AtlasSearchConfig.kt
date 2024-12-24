package com.kogo.content.search

import com.kogo.content.logging.Logger
import com.mongodb.MongoCommandException
import jakarta.annotation.PostConstruct
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@Profile("!local")
class AtlasSearchConfig @Autowired constructor(
    private val applicationContext: ApplicationContext,
    private val mongoTemplate: MongoTemplate
) {
    companion object : Logger() {
        private const val VERSION_COLLECTION = "search_index_versions"
    }

    @PostConstruct
    fun initializeSearchIndexes() {
        if (!mongoTemplate.collectionExists(VERSION_COLLECTION)) {
            mongoTemplate.createCollection(VERSION_COLLECTION)
        }

        val searchIndexBeans = applicationContext.getBeansOfType(SearchIndex::class.java)

        searchIndexBeans.values.forEach { searchIndex ->
            val indexName = searchIndex.indexName()
            val definition = searchIndex.searchIndexDefinition()
            val collectionName = searchIndex.mongoEntityCollectionName()
            val currentVersion = calculateDefinitionVersion(definition)

            when {
                !isIndexExists(indexName, collectionName) -> {
                    createSearchIndex(indexName, collectionName, definition)
                    saveIndexVersion(indexName, currentVersion)
                }
                !isVersionMatch(indexName, currentVersion) -> {
                    updateSearchIndex(indexName, collectionName, definition)
                    saveIndexVersion(indexName, currentVersion)
                }
                else -> {
                    log.info { "Search index '$indexName' exists and is up to date." }
                }
            }
        }
    }

    private fun calculateDefinitionVersion(definition: SearchIndexDefinition): String {
        // Convert definition to Document and sort all fields recursively to ensure consistent ordering
        val sortedDoc = sortDocumentFields(definition.toDocument())
        return sortedDoc.toString().hashCode().toString()
    }

    private fun sortDocumentFields(doc: Document): Document {
        val sortedDoc = Document()
        doc.toSortedMap().forEach { (key, value) ->
            sortedDoc[key] = when (value) {
                is Document -> sortDocumentFields(value)
                is List<*> -> value.map { if (it is Document) sortDocumentFields(it) else it }
                else -> value
            }
        }
        return sortedDoc
    }

    private fun isVersionMatch(indexName: String, currentVersion: String): Boolean {
        val storedVersion = mongoTemplate.db.getCollection(VERSION_COLLECTION)
            .find(Document("indexName", indexName))
            .first()
            ?.getString("version")

        return storedVersion == currentVersion
    }

    private fun isIndexExists(indexName: String, collectionName: String): Boolean {
        return try {
            val pipeline = listOf(
                Document("\$listSearchIndexes", Document("name", indexName))
            )

            val result = mongoTemplate.db
                .getCollection(collectionName)
                .aggregate(pipeline)
                .first()

            result != null && (result.getString("status") == "READY" ||
                             result.getString("status") == "PENDING")
        } catch (e: Exception) {
            log.error { "Error checking index existence: ${e.message}" }
            false
        }
    }

    private fun createSearchIndex(indexName: String, collectionName: String, mapping: SearchIndexDefinition) {
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

            } catch (e: MongoCommandException) {
                if (e.errorCode == 68 && e.errorMessage.contains("Duplicate Index")) {
                    log.info { "Search index '$indexName' already exists, skipping creation." }
                } else {
                    throw e
                }
            } catch (e: Exception) {
                log.error { "Failed to create search index '$indexName': ${e.message}" }
                throw e
            }
        }
        else {
            log.info { "Collection does not exist. Search index $indexName will not be created." }
        }
    }

    private fun updateSearchIndex(indexName: String, collectionName: String, mapping: SearchIndexDefinition) {
        try {
            val command = Document().apply {
                put("updateSearchIndex", collectionName)
                put("name", indexName)
                put("definition", mapping.toDocument())
            }

            mongoTemplate.db.runCommand(command)
            log.info { "Updated search index '$indexName' successfully." }
        } catch (e: Exception) {
            log.error { "Failed to update search index '$indexName': ${e.message}" }
            throw e
        }
    }

    private fun saveIndexVersion(indexName: String, version: String) {
        val versionDoc = Document()
            .append("indexName", indexName)
            .append("version", version)
            .append("updatedAt", java.time.Instant.now())

        val updateCommand = Document().apply {
            put("update", VERSION_COLLECTION)
            put("updates", listOf(Document().apply {
                put("q", Document("indexName", indexName))
                put("u", Document("\$set", versionDoc))
                put("upsert", true)
            }))
        }

        try {
            mongoTemplate.db.runCommand(updateCommand)
        } catch (e: Exception) {
            log.error { "Failed to save index version for '$indexName': ${e.message}" }
            throw e
        }
    }
}