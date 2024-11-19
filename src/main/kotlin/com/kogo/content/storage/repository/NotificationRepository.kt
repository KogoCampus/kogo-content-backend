package com.kogo.content.storage.repository

import com.kogo.content.common.PaginationRequest
import com.kogo.content.common.PaginationSlice
import com.kogo.content.storage.MongoPaginationQueryBuilder
import com.kogo.content.storage.entity.Notification
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.core.MongoTemplate
import kotlin.reflect.KClass

interface NotificationRepository : MongoRepository<Notification, String>, NotificationRepositoryCustom {}

interface NotificationRepositoryCustom {
    fun findAll(paginationRequest: PaginationRequest): PaginationSlice<Notification>
}

class NotificationRepositoryCustomImpl : NotificationRepositoryCustom {

    @Autowired
    private lateinit var mongoPaginationQueryBuilder: MongoPaginationQueryBuilder

    override fun findAll(paginationRequest: PaginationRequest): PaginationSlice<Notification> {
        return mongoPaginationQueryBuilder.getPage(
            Notification::class,
            fieldAlias(),
            paginationRequest = paginationRequest
        )
    }

    private fun fieldAlias(): Map<String, String> = emptyMap()
}
