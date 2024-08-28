package com.kogo.content.service.meilisearch

import com.kogo.content.storage.entity.GroupEntity
import com.kogo.content.storage.entity.PostEntity

interface SearchService {
    fun searchGroups(query: String): List<Map<String, Any>>
    fun searchPosts(query: String): List<PostEntity>
    fun indexGroup(group: GroupEntity)
    fun deleteGroup(groupId: String?)
}