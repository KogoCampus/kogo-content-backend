package com.kogo.content.search.index

import com.kogo.content.search.*
import com.kogo.content.storage.model.entity.Post
import org.springframework.stereotype.Service

@Service
class PostSearchIndex : SearchIndex<Post>(Post::class) {

    override fun defaultSearchConfiguration() = SearchConfiguration(
        textSearchFields = listOf("title", "content", "comments"),
        scoreFields = listOf(
            ScoreField(
                field = "title",
                score = Score.Boost(1.5),
            )
        )
    )
}
