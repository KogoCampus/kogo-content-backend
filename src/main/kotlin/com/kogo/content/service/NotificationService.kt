package com.kogo.content.service

import com.kogo.content.storage.entity.Notification
import com.kogo.content.storage.repository.NotificationRepository
import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
){
    fun createNotification() {}
    fun createPushNotification() {}
    fun getNotifications(paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return notificationRepository.findAll(paginationRequest)
    }
}
