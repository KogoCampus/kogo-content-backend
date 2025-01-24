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
import java.util.concurrent.CompletableFuture

@Service
class PushNotificationService(
    private val notificationRepository: NotificationRepository,
){
    companion object : Logger() {
        private const val PREFIX = "kogocampus://"
    }

    sealed class DeepLink(private val path: String) {
        val url: String get() = PREFIX + path

        data class Post(val postId: String) : DeepLink("post/$postId")
        data class Comment(val postId: String, val commentId: String) : DeepLink("post/$postId/$commentId")
        data class Reply(val postId: String, val commentId: String, val replyId: String) :
            DeepLink("post/$postId/$commentId/$replyId")
        data class Group(val groupId: String) : DeepLink("group/$groupId")
    }

    private data class ExpoPushNotificationRequest(
        val to: String,
        val title: String,
        val body: String,
        val data: ExpoPushNotificationRequestData,
    )

    private data class ExpoPushNotificationRequestData(
        val url: String,
    )

    private val expoPushNotificationApiUrl = "https://exp.host/--/api/v2/push/send"
    private val restTemplate = RestTemplate()

    @Async
    fun dispatchPushNotification(content: Notification, deepLink: DeepLink): CompletableFuture<Notification> {
        return CompletableFuture.supplyAsync {
            val notification = Notification(
                recipient = content.recipient,
                sender = content.sender,
                title = content.title,
                body = content.body
            )

            if (notification.recipient.pushNotificationToken != null) {
                val expoPushNotificationRequest = ExpoPushNotificationRequest(
                    to = notification.recipient.pushNotificationToken!!,
                    title = notification.title,
                    body = notification.body,
                    data = ExpoPushNotificationRequestData(
                        url = deepLink.url
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

    fun getNotificationsByRecipientId(recipientId: String, paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return notificationRepository.findAllByRecipientId(recipientId, paginationRequest)
    }
}


