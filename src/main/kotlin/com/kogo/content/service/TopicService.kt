package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.storage.repository.*
import com.kogo.content.service.util.Transformer
import com.kogo.content.service.util.deleteAttachment
import com.kogo.content.service.util.saveFileAndConvertToAttachment
import com.kogo.content.storage.entity.StudentUserEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KParameter


@Service
class TopicService (
    private val repository: TopicRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandler
) {
    private val transformer: Transformer<TopicDto, TopicEntity> = object : Transformer<TopicDto, TopicEntity>(TopicDto::class, TopicEntity::class) {
        override fun argFor(parameter: KParameter, data: TopicDto): Any? {
            return when (parameter.name) {
                "profileImage" -> data.profileImage?.let {
                    saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) }
                else -> super.argFor(parameter, data)
            }
        }
    }

    fun find(topicId: String): TopicEntity? = repository.findByIdOrNull(topicId)

    fun findByTopicName(topicName: String): TopicEntity? = repository.findByTopicName(topicName)

    @Transactional
    fun create(dto: TopicDto, owner: StudentUserEntity): TopicEntity {
        val topic = transformer.transform(dto)
        topic.owner = owner
        // meilisearchService.indexGroup(newGroup)
        return repository.save(topic)
    }

    @Transactional
    fun update(topic: TopicEntity, topicUpdate: TopicUpdate): TopicEntity {
        with(topicUpdate) {
            topicName?.let { topic.topicName = it }
            description?.let { topic.description = it }
            tags.takeIf { it.isNotEmpty() }?.let { topic.tags = it }
            profileImage?.let { topic.profileImage = saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) }
        }
        return repository.save(topic)
    }

    @Transactional
    fun delete(topic: TopicEntity) {
        topic.profileImage?.let { deleteAttachment(it, attachmentRepository) }
        repository.deleteById(topic.id!!)
    }
}
