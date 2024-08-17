package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class GroupEntity (
    @Id
    var id : String? = null,

    @Indexed(unique = true)
    var groupName: String = "",

    var userCount: Int? = 1,

    var profileImage: ProfileImage? = null,

    var description: String = "",

    var tags: List<String> = emptyList()

) : MongoEntity() {
    companion object {
        fun parseTags(tags: String): List<String> {
            return tags.replace(" ", "")
                .split(",")
                .filter { s -> (s != null && s.length > 0) }
                .toList()
        }
    }
}