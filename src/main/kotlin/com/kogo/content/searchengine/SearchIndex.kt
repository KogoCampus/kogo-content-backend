package com.kogo.content.searchengine

import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic

enum class SearchIndex(val indexId: String) {
    POSTS("posts"),
    TOPICS("topics"),
    COMMENTS("comments")
}
