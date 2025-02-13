package com.kogo.content.storage.model

import com.kogo.content.storage.model.entity.User
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

@Document
data class Notification (
    @Id
    var id: String? = null,

    var type: NotificationType = NotificationType.GENERAL,

    @DocumentReference
    var recipient: User,

    @DocumentReference
    var sender: User?,

    var title: String,

    var body: String,

    var deepLinkUrl: String?,

    var createdAt: Long = System.currentTimeMillis(),
)

enum class NotificationType {
    GENERAL,
    FRIEND_REQUEST
}
