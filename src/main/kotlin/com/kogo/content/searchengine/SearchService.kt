package com.kogo.content.searchengine

import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.Post

interface SearchService {
    fun searchGroups(query: String): List<Map<String, Any>>
    fun searchPosts(query: String): List<Post>
    fun indexGroup(group: Topic)
    fun deleteGroup(groupId: String?)
}
