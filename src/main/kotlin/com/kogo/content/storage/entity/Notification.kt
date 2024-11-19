package com.kogo.content.storage.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Notification (
    @Id
    val id: String? = null,

    val recipientId: String,

    val message: NotificationMessage,

    val isPush: Boolean,
    var createdAt: Instant = Instant.now(),
)

data class NotificationMessage(
    val title: String,
    val body: String,
    val data: Map<String, Any>? = null
)

data class PushNotificationRequest(
    val recipients: List<String>,
    val notification: NotificationMessage
)

