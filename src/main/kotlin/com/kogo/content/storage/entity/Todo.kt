package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document
data class Todo(
    @Id
    var id: String? = null,
    @Field("title")
    var title: String = "",
    @Field("description")
    var description: String = "",
    @Field("completed")
    var completed: Boolean = false
)
