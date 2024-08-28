package com.kogo.content.service.meilisearch

import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.storage.entity.PostEntity
import com.kogo.content.storage.entity.UserEntity
import org.springframework.stereotype.Service
import com.meilisearch.sdk.Client as MeiliSearchClient
import com.meilisearch.sdk.Config
import com.meilisearch.sdk.Index
import com.meilisearch.sdk.SearchRequest
import com.meilisearch.sdk.model.SearchResult
import org.springframework.beans.factory.annotation.Value
import org.json.JSONObject
import org.json.JSONArray
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


@Service
class MeilisearchService(
    @Value("\${meilisearch.host}") private val host: String,
    @Value("\${meilisearch.apiKey}") private val apiKey: String
) : SearchService {

    private val client: MeiliSearchClient = MeiliSearchClient(Config(host, apiKey))

    private val groupIndex: Index = client.index("groups")
    private val postIndex: Index = client.index("posts")

    override fun searchGroups(query: String): List<Map<String, Any>> {
        val searchResult: SearchResult = groupIndex.search(query)

        val gson = Gson()
        val type = object : TypeToken<Map<String, Any>>() {}.type

        return searchResult.hits.map { hit ->
            gson.fromJson(gson.toJson(hit), type) as Map<String, Any>
        }
    }

    override fun searchPosts(query: String): List<PostEntity> {
        TODO()
    }

    override fun indexGroup(group: GroupEntity) {
        val document = JSONObject().apply {
            put("id", group.id)
            put("groupName", group.groupName)
            put("userCount", group.userCount)
            put("profileImage", group.profileImage)
            put("owner", group.owner)
            put("description", group.description)
            put("tags", JSONArray(group.tags))
        }
        val jsonArray = JSONArray().put(document)
        val documents = jsonArray.toString()
        groupIndex.addDocuments(documents)
    }

    override fun deleteGroup(groupId: String?) {
        groupIndex.deleteDocument(groupId)
    }
}
