package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.repository.*
import com.kogo.content.storage.entity.UserDetails
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant


@Service
class TopicService (
    private val repository: TopicRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandler
) {
    fun find(topicId: String): Topic? = repository.findByIdOrNull(topicId)

    fun findByTopicName(topicName: String): Topic? = repository.findByTopicName(topicName)

    fun findByOwnerId(ownerId: String): List<Topic> = repository.findAllByOwnerId(ownerId)

    fun existsByTopicName(topicName: String): Boolean = repository.existsByTopicName(topicName)

    @Transactional
    fun create(dto: TopicDto, owner: UserDetails): Topic {
        val topic = Topic(
            topicName = dto.topicName,
            description = dto.description,
            owner = owner,
            profileImage = dto.profileImage?.let {
                attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository) },
            tags = if (dto.tags.isNullOrEmpty()) emptyList() else dto.tags!!,
            createdAt = Instant.now()
        )
        return repository.save(topic)
    }

    @Transactional
    fun update(topic: Topic, topicUpdate: TopicUpdate): Topic {
        with(topicUpdate) {
            topicName?.let { topic.topicName = it }
            description?.let { topic.description = it }
            tags!!.takeIf { it.isNotEmpty() }?.let { topic.tags = it }
            profileImage?.let { topic.profileImage = attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository) }
        }
        return repository.save(topic)
    }

    @Transactional
    fun delete(topic: Topic) {
        topic.profileImage?.let { attachmentRepository.delete(it) }
        repository.deleteById(topic.id!!)
    }
}
