package com.kogo.content.service

import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.logging.Logger
import com.kogo.content.storage.model.Notification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.*
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CompletableFuture

@Service
class PushNotificationService(
    private val notificationRepository: NotificationRepository,
){
    companion object : Logger()

    sealed class DeepLink(private val path: String) {
        companion object {
            const val PREFIX = "assembli://"

            const val fallback = PREFIX
        }

        val url: String get() = PREFIX + path

        data class Post(val postId: String) : DeepLink("post/$postId")
        data class Comment(val postId: String, val commentId: String) : DeepLink("post/$postId/comment/$commentId")
        data class Reply(val postId: String, val commentId: String, val replyId: String) :
            DeepLink("post/$postId/comment/$commentId/reply/$replyId")
        data class Group(val groupId: String) : DeepLink("group/$groupId")
        data class Friend(val friendUserId: String) : DeepLink("friend/${friendUserId}")
    }

    private val expoPushNotificationApiUrl = "https://exp.host/--/api/v2/push/send"

    private val restTemplate = RestTemplate()

    fun find(notificationId: String) = notificationRepository.findByIdOrNull(notificationId)

    @Async
    fun sendPushNotification(notification: Notification): CompletableFuture<Notification> {
        return CompletableFuture.supplyAsync {
            if (notification.recipient.pushNotificationToken != null) {
                val expoPushNotificationRequest = mapOf(
                    "to" to notification.recipient.pushNotificationToken!!,
                    "title" to notification.title,
                    "body" to notification.body,
                    "data" to mapOf(
                        "url" to (notification.deepLinkUrl)
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

            notificationRepository.save(notification)
        }
    }

    fun getNotificationsByRecipientId(recipientId: String): List<Notification> = notificationRepository.findAllByRecipientId(recipientId)

    fun getNotificationsByRecipientId(recipientId: String, paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return notificationRepository.findAllByRecipientId(recipientId, paginationRequest)
    }

    fun deleteNotification(notificationId: String, recipientId: String) {
        notificationRepository.deleteById(notificationId)
    }
}


