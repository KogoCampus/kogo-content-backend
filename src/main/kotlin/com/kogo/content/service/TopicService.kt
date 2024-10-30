package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.service.search.SearchQueryDao
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.repository.*
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.entity.UserFollowing
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant


@Service
class TopicService(
    private val topicRepository: TopicRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileHandler: FileHandler,
    private val topicSearchDao: SearchQueryDao<Topic>
) : SearchQueryDao<Topic> by topicSearchDao {

    fun find(topicId: String): Topic? = topicRepository.findByIdOrNull(topicId)

    fun getAllTopicsUserFollowingByUserId(userId: String): List<Topic> {
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
        val savedTopic = follow(topicRepository.save(topic), owner)
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
        topicRepository.unfollowAllByFollowableId(topic.id!!)
        topicRepository.deleteById(topic.id!!)
    }

    fun follow(topic: Topic, user: UserDetails): Topic {
        topicRepository.follow(topic.id!!, user.id!!)?.let { topic.followerCount += 1 }
        return topicRepository.save(topic)
    }
    fun unfollow(topic: Topic, user: UserDetails): Topic {
        if(topicRepository.unfollow(topic.id!!, user.id!!)) { topic.followerCount -= 1 }
        return topicRepository.save(topic)
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
    fun findUserFollowing(topic: Topic, user: UserDetails): UserFollowing? = topicRepository.findFollowing(topic.id!!, user.id!!)
}
