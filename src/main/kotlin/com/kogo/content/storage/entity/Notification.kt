package com.kogo.content.storage.entity

import com.kogo.content.endpoint.model.UserData
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Notification (
    @Id
    val id: String? = null,

    val recipientId: String,
    val sender: UserData.Public,
    val eventType: EventType,

    val message: NotificationMessage,

    val isPushNotification: Boolean,
    var createdAt: Instant = Instant.now(),
)

data class NotificationMessage(
    val title: String,
    val body: String,
    val dataType: DataType,
    val data: Any
)

data class PushNotificationRequest(
    val recipients: List<String>,
    val notification: NotificationMessage
)

enum class DataType{
    POST, COMMENT, REPLY, SYSTEM
}

enum class EventType{
    LIKE_TO_POST,
    LIKE_TO_COMMENT,
    LIKE_TO_REPLY,
    CREATE_POST_TO_TOPIC,
    CREATE_COMMENT_TO_POST,
    CREATE_REPLY_TO_COMMENT
}
