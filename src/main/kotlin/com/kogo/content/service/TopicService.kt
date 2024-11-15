package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import com.kogo.content.search.SearchIndex
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.repository.*
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.view.TopicAggregate
import com.kogo.content.storage.view.TopicAggregateView
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TopicService(
    private val topicRepository: TopicRepository,
    private val attachmentRepository: AttachmentRepository,
    private val followerRepository: FollowerRepository,
    private val topicAggregateSearchIndex: SearchIndex<TopicAggregate>,
    private val topicAggregateView: TopicAggregateView
) {

    fun find(topicId: String) = topicRepository.findByIdOrNull(topicId)
    fun findAggregate(topicId: String) = topicAggregateView.find(topicId)

    fun findTopicByTopicName(topicName: String): Topic? = topicRepository.findByTopicName(topicName)
    fun findTopicsByOwner(owner: User) = topicRepository.findAllByOwnerId(owner.id!!)

    fun getAllFollowingTopicsByUserId(userId: String): List<Topic> {
        val followings = followerRepository.findAllFollowingsByUserId(userId)
        return followings.map { following -> find(following.followableId.toString()) }.filterNotNull()
    }

    fun searchTopicAggregatesByKeyword(
        searchText: String,
        paginationRequest: PaginationRequest
    ): PaginationSlice<TopicAggregate> {
        return topicAggregateSearchIndex.search(
            searchText = searchText,
            paginationRequest = paginationRequest,
            boost = 1.0  // Normal relevance boost
        )
    }

    @Transactional
    fun create(dto: TopicDto, owner: User): Topic {
        val savedTopic = topicRepository.save(
            Topic(
                topicName = dto.topicName,
                description = dto.description,
                owner = owner,
                profileImage = dto.profileImage?.let { attachmentRepository.saveFile(it) },
                tags = if (dto.tags.isNullOrEmpty()) emptyList() else dto.tags!!,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        )
        topicAggregateView.refreshView(savedTopic.id!!)
        return savedTopic
    }

    @Transactional
    fun update(topic: Topic, topicUpdate: TopicUpdate): Topic {
        with(topicUpdate) {
            topicName?.let { topic.topicName = it }
            description?.let { topic.description = it }
            tags!!.takeIf { it.isNotEmpty() }?.let { topic.tags = it }
            profileImage?.let { topic.profileImage = attachmentRepository.saveFile(it) }
        }
        topic.updatedAt = Instant.now()
        val updatedTopic = topicRepository.save(topic)
        topicAggregateView.refreshView(topic.id!!)
        return updatedTopic
    }

    @Transactional
    fun delete(topic: Topic) {
        topic.profileImage?.let { attachmentRepository.delete(it) }
        followerRepository.unfollowAllByFollowableId(topic.id!!)
        topicRepository.deleteById(topic.id!!)
    }

    @Transactional
    fun follow(topic: Topic, user: User): Topic {
        val follower = followerRepository.follow(topic.id!!, user.id!!)
        if (follower != null) {
            topicAggregateView.refreshView(topic.id!!)
        }
        return topic
    }

    @Transactional
    fun unfollow(topic: Topic, user: User): Topic {
        val unfollowed = followerRepository.unfollow(topic.id!!, user.id!!)
        if (unfollowed) {
            topicAggregateView.refreshView(topic.id!!)
        }
        return topic
    }

    fun transferOwnership(topic: Topic, user: User): Topic {
        topic.owner = user
        topicRepository.save(topic)
        topicAggregateView.refreshView(topic.id!!)
        return topic
    }

    fun hasUserFollowedTopic(topic: Topic, user: User): Boolean {
        return followerRepository.findFollowing(topic.id!!, user.id!!) != null
    }

    fun isUserTopicOwner(topic: Topic, owner: User): Boolean = topic.owner == owner
}
