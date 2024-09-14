package com.kogo.content.searchengine

/*

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

    override fun indexGroup(group: TopicEntity) {
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
*/
