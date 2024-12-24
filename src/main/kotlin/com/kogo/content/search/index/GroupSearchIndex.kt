package com.kogo.content.search.index

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.search.*
import com.kogo.content.storage.model.entity.Group
import org.springframework.stereotype.Service

@Service
class GroupSearchIndex : SearchIndex<Group>(Group::class) {

    override fun defaultSearchConfiguration() = SearchConfiguration(
        textSearchFields = listOf(
            "groupName",
            "description",
            "tags"
        ),
        scoreFields = listOf(
            ScoreField(
                field = "groupName",
                score = Score.Boost(1.25)
            ),
            ScoreField(
                field = "tags",
                score = Score.Boost(1.25)
            )
        )
    )
}
