package com.kogo.content.service

import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.endpoint.model.UserData
import com.kogo.content.logging.Logger
import com.kogo.content.storage.entity.*
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
){
    companion object : Logger()

    private val restTemplate = RestTemplate()

    fun updatePushToken(recipientId: String, pushToken: String): User {
        val updatingUser = userRepository.findUserById(recipientId)
        updatingUser!!.pushToken = pushToken
        return userRepository.save(updatingUser)
    }

    fun createNotification(recipientId: String, sender: User, eventType:EventType, message: NotificationMessage): Notification {
        val newNotification = Notification(
            recipientId = recipientId,
            sender = UserData.Public.from(sender),
            eventType = eventType,
            message = message,
            isPushNotification = false,
        )
        return notificationRepository.save(newNotification)
    }

    fun createPushNotification(recipientId: String, sender: User, eventType:EventType, message: NotificationMessage): Notification {
        val newNotification = Notification(
            recipientId = recipientId,
            sender = UserData.Public.from(sender),
            eventType = eventType,
            message = message,
            isPushNotification = true,
        )

        return notificationRepository.save(newNotification)
    }

    fun getNotificationsByRecipientEmail(email: String, paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return notificationRepository.findAllByRecipientId(email,paginationRequest)
    }
}


