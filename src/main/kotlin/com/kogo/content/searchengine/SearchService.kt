package com.kogo.content.searchengine

import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.storage.entity.PostEntity

interface SearchService {
    fun searchGroups(query: String): List<Map<String, Any>>
    fun searchPosts(query: String): List<PostEntity>
    fun indexGroup(group: TopicEntity)
    fun deleteGroup(groupId: String?)
}