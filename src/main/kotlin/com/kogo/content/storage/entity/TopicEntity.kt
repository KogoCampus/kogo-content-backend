package com.kogo.content.storage.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document

@Document
@CompoundIndex(def = "{'owner.id': 1}")
data class TopicEntity (
    @Id
    var id : String? = null,

    @Indexed(unique = true)
    var groupName: String = "",

    var userCount: Int = 1,

    @DBRef
    var profileImage: Attachment ?= null,

    @DBRef
    @JsonBackReference
    var owner: StudentUserEntity? = null,

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