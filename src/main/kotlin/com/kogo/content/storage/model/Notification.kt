package com.kogo.content.storage.model

import com.kogo.content.service.PushNotificationService
import com.kogo.content.storage.model.entity.User
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

@Document
data class Notification (
    @Id
    val id: String? = null,

    @DocumentReference
    val recipient: User,

    @DocumentReference
    val sender: User?,

    val title: String,

    val body: String,

    val deepLink: String,

    var createdAt: Long = System.currentTimeMillis(),
)
