package com.kogo.content.service

import com.kogo.content.storage.entity.Notification
import com.kogo.content.storage.entity.PushNotificationRequest
import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.storage.entity.NotificationMessage
import com.kogo.content.storage.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    @Value("\${kogo-api.base-url}") private val apiBaseUrl: String,
    @Value("\${kogo-api.push-notification-endpoint}") private val pushNotificationEndpoint: String
){
    private val restTemplate = RestTemplate()

    fun createNotification(recipientId: String, message: NotificationMessage): Notification {
        val newNotification = Notification(
            recipientId = recipientId,
            message = message,
            isPushNotification = false,
        )
        return notificationRepository.save(newNotification)
    }

//    fun createNotifications(recipientIds: List<String>): List<Notification> {
//
//    }

    fun createPushNotification(recipientId: String, message: NotificationMessage): Notification {
        val newNotification = Notification(
            recipientId = recipientId,
            message = message,
            isPushNotification = true,
        )

        // Save the notification to the database first
        val savedNotification = notificationRepository.save(newNotification)

        // Retrieve recipient ID Token
        val recipient = userRepository.findUserById(newNotification.recipientId)

        // Prepare the PushNotificationRequest
        val pushNotificationRequest = PushNotificationRequest(
            recipients = listOf(recipient?.idToken.toString()),
            notification = savedNotification.message
        )
        // Make the API call to push notifications
        val url = "$apiBaseUrl$pushNotificationEndpoint"
        val headers = HttpHeaders()
        val entity = HttpEntity(pushNotificationRequest, headers)

        // Perform the API call
        val response: ResponseEntity<String> = restTemplate.exchange(
            url, HttpMethod.POST, entity, String::class.java
        )

        // Check response (You can also add error handling or logging here)
        if (response.statusCode.is2xxSuccessful) {
            println("Push notification sent successfully.")
        } else {
            println("Failed to send push notification: ${response.statusCode}")
        }

        return savedNotification
    }
    fun getNotificationsByRecipientId(recipientId: String, paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return notificationRepository.findAllByRecipientId(recipientId,paginationRequest)
    }
}


