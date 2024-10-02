package com.kogo.content.searchengine

import com.fasterxml.jackson.databind.node.ObjectNode
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.web.client.RestTemplate

@SpringBootTest
class MeilisearchClientTest {

    private val restTemplate: RestTemplate = mockk()
    private lateinit var meilisearchClient: MeilisearchClient

    @BeforeEach
    fun setUp() {
        meilisearchClient = spyk(MeilisearchClient())  // Inject the mock RestTemplate

        meilisearchClient.apply {
            this::class.java.getDeclaredField("restTemplate").apply {
                isAccessible = true
                set(meilisearchClient, restTemplate)
            }
        }

        meilisearchClient.meilisearchHost = "http://localhost:7700"
        meilisearchClient.masterKey = "masterKey"
        meilisearchClient.apiKey = ""

        meilisearchClient.init()

        // Mocking the exchange method of restTemplate
        every { restTemplate.exchange(any<String>(), any(), any(), any<Class<String>>()) } returns mockk()
    }

    @Test
    fun `should use master key when apiKey is empty`() {
        val entitySlot = slot<HttpEntity<*>>()
        val document = Document("1").apply { put("title", "Test") }
        val index = SearchIndex.POSTS

        val responseEntity = ResponseEntity("Success", HttpStatus.OK)

        every {
            restTemplate.exchange(
                any<String>(),
                any(),
                capture(entitySlot),
                any<Class<String>>()
            )
        } returns responseEntity

        meilisearchClient.addDocument(index, document)

        val capturedHeaders = entitySlot.captured.headers
        assertThat(capturedHeaders["Authorization"]).containsExactly("Bearer masterKey")
    }

    @Test
    fun `should add document to Meilisearch index`() {
        val entitySlot = slot<HttpEntity<*>>()
        val document = Document("1").apply { put("title", "Test Document") }
        val index = SearchIndex.POSTS

        val responseEntity = ResponseEntity("Success", HttpStatus.OK)

        every {
            restTemplate.exchange(
                "http://localhost:7700/indexes/posts/documents",
                HttpMethod.POST,
                capture(entitySlot),
                any<Class<String>>()
            )
        } returns responseEntity

        meilisearchClient.addDocument(index, document)

        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.body).isInstanceOf(ObjectNode::class.java)

        val jsonBody = capturedEntity.body as ObjectNode
        assertThat(jsonBody.get("id").asText()).isEqualTo("1")
        assertThat(jsonBody.get("title").asText()).isEqualTo("Test Document")
    }

    @Test
    fun `should update document in Meilisearch index`() {
        val entitySlot = slot<HttpEntity<*>>()
        val document = Document("1").apply { put("title", "Updated Document") }
        val index = SearchIndex.POSTS

        val responseEntity = ResponseEntity("Success", HttpStatus.OK)

        every {
            restTemplate.exchange(
                "http://localhost:7700/indexes/posts/documents",
                HttpMethod.PUT,
                capture(entitySlot),
                any<Class<String>>()
            )
        } returns responseEntity

        meilisearchClient.updateDocument(index, document)

        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.body).isInstanceOf(ObjectNode::class.java)

        val jsonBody = capturedEntity.body as ObjectNode
        assertThat(jsonBody.get("id").asText()).isEqualTo("1")
        assertThat(jsonBody.get("title").asText()).isEqualTo("Updated Document")
    }

    @Test
    fun `should delete document from Meilisearch index`() {
        val entitySlot = slot<HttpEntity<*>>()
        val index = SearchIndex.POSTS
        val documentId = "1"

        every { meilisearchClient.searchDocument(index, documentId) } returns documentId

        val responseEntity = ResponseEntity("Success", HttpStatus.OK)

        every {
            restTemplate.exchange(
                "http://localhost:7700/indexes/posts/documents/1",
                HttpMethod.DELETE,
                capture(entitySlot),
                any<Class<String>>()
            )
        } returns responseEntity

        meilisearchClient.deleteDocument(index, documentId)

        verify {
            restTemplate.exchange(
                "http://localhost:7700/indexes/posts/documents/1",
                HttpMethod.DELETE,
                any<HttpEntity<*>>(),
                any<Class<String>>()
            )
        }
    }

    @Test
    fun `should search documents across multiple indexes`() {
        val entitySlot = slot<HttpEntity<*>>()
        val indexes = listOf(SearchIndex.POSTS, SearchIndex.COMMENTS)
        val queryOptions = "first"

        val responseJson = """
            {
              "results": [
                {
                  "indexUid": "posts",
                  "hits": [
                    {"id": "1", "title": "First Post", "content": "Content of the first post", "authorId": "author1", "topicId": "topic1"}
                  ]
                },
                {
                  "indexUid": "comments",
                  "hits": [
                    {"id": "2", "parentId": "1", "parentType": "POST", "content": "First comment", "authorId": "author2"}
                  ]
                }
              ]
            }
        """

        val responseEntity = ResponseEntity(responseJson, HttpStatus.OK)

        every {
            restTemplate.exchange(
                "http://localhost:7700/multi-search",
                HttpMethod.POST,
                capture(entitySlot),
                any<Class<String>>()
            )
        } returns responseEntity

        val result = meilisearchClient.searchDocuments(indexes, queryOptions)

        // Verify the request entity
        val capturedEntity = entitySlot.captured
        assertThat(capturedEntity.body).isInstanceOf(ObjectNode::class.java)

        // Verify the contents of the request body
        val requestBody = capturedEntity.body as ObjectNode
        assertThat(requestBody.get("queries")).isNotNull

        // Validate the returned result map
        assertThat(result).containsKeys(SearchIndex.POSTS, SearchIndex.COMMENTS)

        val postDocuments = result[SearchIndex.POSTS]
        assertThat(postDocuments).hasSize(1)
        assertThat(postDocuments?.get(0)?.documentId).isEqualTo("1")
        assertThat(postDocuments?.get(0)?.toJsonNode()?.get("title")?.asText()).isEqualTo("First Post")

        val commentDocuments = result[SearchIndex.COMMENTS]
        assertThat(commentDocuments).hasSize(1)
        assertThat(commentDocuments?.get(0)?.documentId).isEqualTo("2")
        assertThat(commentDocuments?.get(0)?.toJsonNode()?.get("content")?.asText()).isEqualTo("First comment")
    }
}

