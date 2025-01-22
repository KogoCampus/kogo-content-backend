package com.kogo.content.service

import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.logging.Logger
import com.kogo.content.storage.model.Notification
import org.springframework.http.*
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class PushNotificationService(
    private val notificationRepository: NotificationRepository,
){
    companion object : Logger() {
        private const val PREFIX = "kogocampus://"

        fun linkToPost(postId: String) = PREFIX + "post/$postId"
        fun linkToComment(postId: String, commentId: String) = PREFIX + "post/$postId/comment/$commentId"
        fun linkToReply(postId: String, commentId: String, replyId: String) = PREFIX + "post/$postId/comment/$commentId/reply/$replyId"
        fun linkToGroup(groupId: String) = PREFIX + "group/$groupId"
    }

    data class ExpoPushNotificationRequest(
        val to: String,
        val title: String,
        val body: String,
        val data: ExpoPushNotificationRequestData,
    )

    data class ExpoPushNotificationRequestData (
        val url: String,
    )

    private val expoPushNotificationApiUrl = "https://exp.host/--/api/v2/push/send"

    private val restTemplate = RestTemplate()

    @Async
    fun createPushNotification(notification: Notification, linkingUrl: String): Notification {
        if (notification.recipient.pushNotificationToken != null) {
            val expoPushNotificationRequest = ExpoPushNotificationRequest(
                to = notification.recipient.pushNotificationToken!!,
                title = notification.title,
                body = notification.body,
                data = ExpoPushNotificationRequestData(
                    url = linkingUrl
                )
            )

            val requestEntity = HttpEntity(expoPushNotificationRequest, HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            })

            val response: ResponseEntity<String> = restTemplate.postForEntity(expoPushNotificationApiUrl, requestEntity, String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                log.info{"Push notification sent successfully - notification id ${notification.id}"}
            } else {
                log.error{"Failed to send push notification - notification id ${notification.id}"}
            }
        }

        return notificationRepository.save(notification)
    }

    fun getNotificationsByRecipientId(recipientId: String, paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return notificationRepository.findAllByRecipientId(recipientId, paginationRequest)
    }
}


