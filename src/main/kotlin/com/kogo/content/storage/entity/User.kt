package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class User (
    @Id
    var id: String? = "",
    var username: String? = "",
    var email: String? = "",
)