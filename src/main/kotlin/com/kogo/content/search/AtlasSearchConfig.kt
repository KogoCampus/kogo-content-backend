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
            val currentVersion = calculateDefinitionVersion(searchIndex.searchIndexDefinition())

            when {
                !isIndexExists(searchIndex) -> {
                    createSearchIndex(searchIndex)
                    saveIndexVersion(searchIndex.indexName(), currentVersion)
                }
                !isVersionMatch(searchIndex.indexName(), currentVersion) -> {
                    updateSearchIndex(searchIndex)
                    saveIndexVersion(searchIndex.indexName(), currentVersion)
                }
                else -> {
                    log.info { "Search index '${searchIndex.indexName()}' exists and is up to date." }
                }
            }
        }
    }

    private fun calculateDefinitionVersion(definition: SearchIndexDefinition): String {
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

    private fun isIndexExists(searchIndex: SearchIndex<*>): Boolean {
        return try {
            val pipeline = listOf(
                Document("\$listSearchIndexes", Document("name", searchIndex.indexName()))
            )

            val result = mongoTemplate.db
                .getCollection(searchIndex.mongoEntityCollectionName())
                .aggregate(pipeline)
                .first()

            result != null && (result.getString("status") == "READY" ||
                             result.getString("status") == "PENDING")
        } catch (e: Exception) {
            log.error { "Error checking index existence: ${e.message}" }
            false
        }
    }

    private fun createSearchIndex(searchIndex: SearchIndex<*>) {
        if (!mongoTemplate.collectionExists(searchIndex.mongoEntityCollectionName())) {
            log.info { "Collection '${searchIndex.mongoEntityCollectionName()}' does not exist. Creating collection..." }
            mongoTemplate.createCollection(searchIndex.entity.java)
        }

        val command = Document().apply {
            put("createSearchIndexes", searchIndex.mongoEntityCollectionName())
            put("indexes", listOf(Document().apply {
                put("name", searchIndex.indexName())
                put("definition", searchIndex.searchIndexDefinition().toDocument())
            }))
        }

        try {
            mongoTemplate.db.runCommand(command)
            log.info { "Search index '${searchIndex.indexName()}' created successfully." }

        } catch (e: MongoCommandException) {
            if (e.errorCode == 68 && e.errorMessage.contains("Duplicate Index")) {
                log.info { "Search index '${searchIndex.indexName()}' already exists, skipping creation." }
            } else {
                throw e
            }
        } catch (e: Exception) {
            log.error { "Failed to create search index '${searchIndex.indexName()}': ${e.message}" }
            throw e
        }
    }

    private fun updateSearchIndex(searchIndex: SearchIndex<*>) {
        try {
            val command = Document().apply {
                put("updateSearchIndex", searchIndex.mongoEntityCollectionName())
                put("name", searchIndex.indexName())
                put("definition", searchIndex.searchIndexDefinition().toDocument())
            }

            mongoTemplate.db.runCommand(command)
            log.info { "Updated search index '${searchIndex.indexName()}' successfully." }
        } catch (e: Exception) {
            log.error { "Failed to update search index '${searchIndex.indexName()}': ${e.message}" }
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
