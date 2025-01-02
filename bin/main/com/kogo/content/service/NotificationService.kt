package com.kogo.content.service

import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.model.UserData
import com.kogo.content.logging.Logger
import com.kogo.content.storage.model.EventType
import com.kogo.content.storage.model.Notification
import com.kogo.content.storage.model.NotificationMessage
import com.kogo.content.storage.model.PushNotificationRequest
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.repository.UserRepository
import org.springframework.http.*
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
){
    companion object : Logger()

    private val restTemplate = RestTemplate()

    fun updatePushToken(recipientId: String, pushToken: String): User {
        val updatingUser = userRepository.findUserById(recipientId)
        updatingUser!!.pushNotificationToken = pushToken
        return userRepository.save(updatingUser)
    }

    fun createNotification(recipientId: String, sender: User, eventType: EventType, message: NotificationMessage): Notification {
        val notification = Notification(
            recipientId = recipientId,
            sender = UserData.Public.from(sender),
            eventType = eventType,
            message = message,
            isPushNotification = false,
        )
        return notificationRepository.save(notification)
    }

    @Async
    fun createPushNotification(recipientId: String, sender: User, eventType: EventType, message: NotificationMessage): Notification {
        val notification = Notification(
            recipientId = recipientId,
            sender = UserData.Public.from(sender),
            eventType = eventType,
            message = message,
            isPushNotification = true,
        )
        val recipientPushToken = userRepository.findUserById(recipientId)?.pushNotificationToken ?: return notification
        val newNotification = notificationRepository.save(notification)

        // Send the push notification to the Expo server
        val pushNotificationRequest = PushNotificationRequest(
            to = recipientPushToken!!,
            title = message.title,
            body = message.body,
            data = message.data,
            dataType = message.dataType.name
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val requestEntity = HttpEntity(pushNotificationRequest, headers)
        val expoApiUrl = "https://exp.host/--/api/v2/push/send"

        try {
            val response: ResponseEntity<String> = restTemplate.postForEntity(expoApiUrl, requestEntity, String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                log.info{"Push notification sent successfully to $recipientId: ${response.body}"}
            } else {
                log.error{"Failed to send push notification to $recipientId: ${response.body}"}
            }
        } catch (ex: Exception) {
            log.error(ex){"Error while sending push notification to $recipientId: ${ex.message}"}
        }

        return newNotification
    }

    fun getNotificationsByRecipientId(recipientId: String, paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return notificationRepository.findAllByRecipientId(recipientId, paginationRequest)
    }
}


