package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.repository.*
import com.kogo.content.service.util.Transformer
import com.kogo.content.service.util.deleteAttachment
import com.kogo.content.service.util.saveFileAndConvertToAttachment
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
    private val transformer: Transformer<TopicDto, Topic> = object : Transformer<TopicDto, Topic>(TopicDto::class, Topic::class) {
        override fun argFor(parameter: KParameter, data: TopicDto): Any? {
            return when (parameter.name) {
                "profileImage" -> data.profileImage?.let {
                    saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) }
                else -> super.argFor(parameter, data)
            }
        }
    }

    fun find(topicId: String): Topic? = repository.findByIdOrNull(topicId)

    fun findByTopicName(topicName: String): Topic? = repository.findByTopicName(topicName)

    fun findByOwnerId(ownerId: String): List<Topic> = repository.findByOwner(ownerId)

    fun existsByTopicName(topicName: String): Boolean = repository.existsByTopicName(topicName)

    @Transactional
    fun create(dto: TopicDto, owner: String): Topic {
        val topic = Topic(
            topicName = dto.topicName,
            description = dto.description,
            owner = owner,
            profileImage = dto.profileImage?.let {
                saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) },
            tags = if (dto.tags.isNullOrEmpty()) emptyList() else dto.tags!!
        )
        // meilisearchService.indexGroup(newGroup)
        return repository.save(topic)
    }

    @Transactional
    fun update(topic: Topic, topicUpdate: TopicUpdate): Topic {
        with(topicUpdate) {
            topicName?.let { topic.topicName = it }
            description?.let { topic.description = it }
            tags!!.takeIf { it.isNotEmpty() }?.let { topic.tags = it }
            profileImage?.let { topic.profileImage = saveFileAndConvertToAttachment(it, fileHandler, attachmentRepository) }
        }
        return repository.save(topic)
    }

    @Transactional
    fun delete(topic: Topic) {
        topic.profileImage?.let { deleteAttachment(it, attachmentRepository) }
        repository.deleteById(topic.id!!)
    }
}
