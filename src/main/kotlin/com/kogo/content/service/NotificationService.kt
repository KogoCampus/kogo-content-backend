package com.kogo.content.service

import com.kogo.content.storage.entity.Notification
import com.kogo.content.storage.entity.PushNotificationRequest
import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.logging.Logger
import com.kogo.content.storage.entity.NotificationMessage
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    @Value("\${kogo-api.push-notification}") private val pushNotificationEndpoint: String
){
    companion object : Logger()

    private val restTemplate = RestTemplate()

    fun updatePushToken(recipientId: String, pushToken: String): User {
        val updatingUser = userRepository.findUserById(recipientId)
        updatingUser!!.pushToken = pushToken
        return userRepository.save(updatingUser)
    }

    fun createNotification(recipientId: String, message: NotificationMessage): Notification {
        val newNotification = Notification(
            recipientId = recipientId,
            message = message,
            isPushNotification = false,
        )
        return notificationRepository.save(newNotification)
    }

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
        val url = "$pushNotificationEndpoint"
        val headers = HttpHeaders()
        val entity = HttpEntity(pushNotificationRequest, headers)

        // Perform the API call
        try {
            val response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String::class.java
            )

            if (!response.statusCode.is2xxSuccessful) {
                throw RestClientException(
                    "Push notification request failed with status: ${response.statusCode}, body: ${response.body}"
                )
            }

            log.info { "Push notification sent successfully to user $recipientId" }
        } catch (e: RestClientException) {
            log.error(e) { "Failed to send push notification to user $recipientId: ${e.message}" }
        }

        return savedNotification
    }

    fun getNotificationsByRecipientId(recipientId: String, paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return notificationRepository.findAllByRecipientId(recipientId,paginationRequest)
    }
}


