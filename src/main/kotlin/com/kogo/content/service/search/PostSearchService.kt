package com.kogo.content.service.search

import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse
import com.kogo.content.storage.entity.Post
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate

// class PostSearchService (private val mongoTemplate: MongoTemplate) : SearchService {
//
//     companion object {
//         const val COLLECTION_NAME = "posts"
//         const val SEARCH_INDEX = "search"
//     }
//
//     private val postCollection: MongoCollection<Post> = mongoTemplate.db.getCollection(COLLECTION_NAME, Post::class.java)
//
//     override fun searchByKeyword(keyword: String): List<*> {
//
//     }
//
//     override fun searchByKeyword(keyword: String, paginationRequest: PaginationRequest): PaginationResponse<*> {
//         val limit = paginationRequest.limit
//         val pageLastResourceId = paginationRequest.pageToken.pageLastResourceId
//
//         // Define the $search stage for keyword search in title and content
//         val searchStage = Document("\$search", Document.parse("""
//             {
//                 "index": "$SEARCH_INDEX"
//                 "text": {
//                     "query": "$keyword",
//                     "path": ["title", "content"]
//                 }
//             }
//         """))
//
//         // Execute the aggregation pipeline and collect results
//         val posts = postCollection.aggregate(aggregationPipeline).into(ArrayList())
//
//         // Determine the next page token based on the last post ID in the results
//         val nextPageToken = posts.lastOrNull()?.let {
//             paginationRequest.pageToken.nextPageToken(it.id!!)
//         }
//
//         return PaginationResponse(posts, nextPageToken)
//     }
// }
//
