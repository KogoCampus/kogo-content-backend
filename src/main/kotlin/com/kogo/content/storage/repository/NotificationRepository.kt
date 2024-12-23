package com.kogo.content.storage.repository

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.storage.pagination.MongoPaginationQueryBuilder
import com.kogo.content.storage.model.Notification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.repository.MongoRepository

interface NotificationRepository : MongoRepository<Notification, String>, NotificationRepositoryCustom {}

interface NotificationRepositoryCustom {
    fun findAllByRecipientId(recipientId:String, paginationRequest: PaginationRequest): PaginationSlice<Notification>
}

class NotificationRepositoryCustomImpl : NotificationRepositoryCustom {

    @Autowired
    private lateinit var mongoPaginationQueryBuilder: MongoPaginationQueryBuilder

    override fun findAllByRecipientId(recipientId:String, paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return mongoPaginationQueryBuilder.getPage(
            Notification::class,
            paginationRequest = paginationRequest.withFilter("recipientId", recipientId)
        )
    }
}
