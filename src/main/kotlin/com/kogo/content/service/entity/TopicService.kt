package com.kogo.content.service.entity

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.service.filehandler.FileHandler
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.repository.*
import com.kogo.content.storage.entity.UserDetails
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant


@Service
class TopicService(
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandler,
    private val topicRepository: TopicRepository,
) {
    fun find(topicId: String): Topic? = topicRepository.findByIdOrNull(topicId)

    fun listFollowingTopicsByUserId(userId: String): List<Topic> {
        val followings = topicRepository.findAllFollowingsByUserId(userId)
        val topics = followings.mapNotNull { following -> find(following.followableId) }
        return topics
    }

    @Transactional
    fun create(dto: TopicDto, owner: UserDetails): Topic {
        val topic = Topic(
            topicName = dto.topicName,
            description = dto.description,
            owner = owner,
            profileImage = dto.profileImage?.let {
                attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository) },
            tags = if (dto.tags.isNullOrEmpty()) emptyList() else dto.tags!!,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val savedTopic = topicRepository.save(topic)
        topicRepository.follow(followableId = topic.id!!, userId = owner.id!!)
        return savedTopic
    }

    @Transactional
    fun update(topic: Topic, topicUpdate: TopicUpdate): Topic {
        with(topicUpdate) {
            topicName?.let { topic.topicName = it }
            description?.let { topic.description = it }
            tags!!.takeIf { it.isNotEmpty() }?.let { topic.tags = it }
            profileImage?.let { topic.profileImage = attachmentRepository.saveFileAndReturnAttachment(it, fileHandler, attachmentRepository) }
        }
        topic.updatedAt = Instant.now()
        return topicRepository.save(topic)
    }

    @Transactional
    fun delete(topic: Topic) {
        topic.profileImage?.let { attachmentRepository.delete(it) }
        topicRepository.deleteById(topic.id!!)
        topicRepository.unfollowAllByFollowableId(topic.id!!)
    }

    fun follow(topic: Topic, user: UserDetails) {
        topicRepository.follow(topic.id!!, user.id!!)?.let {
            topic.followingUserCount += 1
            topicRepository.save(topic)
        }
    }
    fun unfollow(topic: Topic, user: UserDetails) {
        if(topicRepository.unfollow(topic.id!!, user.id!!)) {
            topic.followingUserCount -= 1
            topicRepository.save(topic)
        }
    }

    fun transferOwnership(topic: Topic, user: UserDetails): Topic {
        topic.owner = user
        return topicRepository.save(topic)
    }

    fun isTopicExist(topicName: String): Boolean = topicRepository.existsByTopicName(topicName)
    fun isUserFollowingTopic(topic: Topic, user: UserDetails): Boolean {
        return topicRepository.findFollowing(topic.id!!, user.id!!) != null
    }
    fun isTopicOwner(topic: Topic, owner: UserDetails): Boolean = topic.owner == owner
}
