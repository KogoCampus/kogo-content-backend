package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.lib.*
import com.kogo.content.search.SearchIndex
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.entity.Follower
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.FollowerRepository
import com.kogo.content.storage.repository.TopicRepository
import com.kogo.content.storage.view.TopicAggregate
import com.kogo.content.storage.view.TopicAggregateView
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.time.Instant

class TopicServiceTest {

    private val topicRepository: TopicRepository = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()
    private val followerRepository: FollowerRepository = mockk()
    private val topicAggregateView: TopicAggregateView = mockk()
    private val topicAggregateSearchIndex: SearchIndex<TopicAggregate> = mockk()

    private val topicService = TopicService(
        topicRepository = topicRepository,
        attachmentRepository = attachmentRepository,
        followerRepository = followerRepository,
        topicAggregateView = topicAggregateView,
        topicAggregateSearchIndex = topicAggregateSearchIndex
    )

    @Test
    fun `should create new topic with auto-follow`() {
        val owner = mockk<User> { every { id } returns "test-owner-id" }
        val profileImage = MockMultipartFile("image", "test.jpg", "image/jpeg", "test".toByteArray())
        val attachment = mockk<Attachment>()
        val topicDto = TopicDto(
            topicName = "Test Topic",
            description = "Test Description",
            profileImage = profileImage,
            tags = listOf("tag1", "tag2")
        )
        val savedTopic = Topic(
            id = "test-topic-id",
            topicName = topicDto.topicName,
            description = topicDto.description,
            owner = owner,
            profileImage = attachment,
            tags = topicDto.tags!!,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { attachmentRepository.saveFile(profileImage) } returns attachment
        every { topicRepository.save(any()) } returns savedTopic
        every { followerRepository.follow("test-topic-id", "test-owner-id") } returns mockk()
        every { topicAggregateView.refreshView("test-topic-id") } returns mockk()

        val result = topicService.create(topicDto, owner)

        assertThat(result).isEqualTo(savedTopic)
        verify {
            attachmentRepository.saveFile(profileImage)
            topicRepository.save(any())
            topicAggregateView.refreshView("test-topic-id")
        }
    }

    @Test
    fun `should update existing topic`() {
        val topic = Topic(
            id = "test-topic-id",
            topicName = "Original Name",
            description = "Original Description",
            owner = mockk(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(1800)
        )
        val update = TopicUpdate(
            topicName = "Updated Name",
            description = "Updated Description",
            tags = listOf("new-tag"),
            profileImage = MockMultipartFile("image", "test.jpg", "image/jpeg", "test".toByteArray())
        )
        val attachment = mockk<Attachment>()

        every { attachmentRepository.saveFile(update.profileImage!!) } returns attachment
        every { topicRepository.save(any()) } returns topic
        every { topicAggregateView.refreshView("test-topic-id") } returns mockk()

        val result = topicService.update(topic, update)

        assertThat(result.topicName).isEqualTo(update.topicName)
        assertThat(result.description).isEqualTo(update.description)
        verify {
            attachmentRepository.saveFile(update.profileImage!!)
            topicRepository.save(any())
            topicAggregateView.refreshView("test-topic-id")
        }
    }

    @Test
    fun `should search topics by keyword`() {
        val searchText = "test"
        val paginationRequest = PaginationRequest(limit = 10)
        val topicAggregates = listOf(
            TopicAggregate(topicId = "topic-1", topic = mockk(), followerCount = 10, postCount = 5),
            TopicAggregate(topicId = "topic-2", topic = mockk(), followerCount = 20, postCount = 10)
        )
        val paginationSlice = PaginationSlice(
            items = topicAggregates,
            nextPageToken = PageToken(
                cursors = mapOf("id" to CursorValue("topic-2", CursorValueType.STRING))
            )
        )

        every {
            topicAggregateSearchIndex.search(
                searchText = searchText,
                paginationRequest = paginationRequest,
                boost = 1.0
            )
        } returns paginationSlice

        val result = topicService.searchTopicAggregatesByKeyword(searchText, paginationRequest)

        assertThat(result.items).hasSize(2)
        verify { topicAggregateSearchIndex.search(searchText, paginationRequest, 1.0) }
    }

    @Test
    fun `should handle follow operations correctly`() {
        val topic = mockk<Topic> { every { id } returns "test-topic-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test successful follow operation
        val follower = mockk<Follower>()
        every { followerRepository.follow("test-topic-id", "test-user-id") } returns follower
        every { topicAggregateView.refreshView("test-topic-id") } returns mockk<TopicAggregate>()

        topicService.follow(topic, user)
        verify {
            followerRepository.follow("test-topic-id", "test-user-id")
            topicAggregateView.refreshView("test-topic-id")
        }

        // Test unsuccessful follow operation (already following)
        every { followerRepository.follow("test-topic-id", "test-user-id") } returns null

        topicService.follow(topic, user)
        verify(exactly = 1) { topicAggregateView.refreshView("test-topic-id") } // Should not be called again
    }

    @Test
    fun `should handle unfollow operations correctly`() {
        val topic = mockk<Topic> { every { id } returns "test-topic-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test successful unfollow operation
        every { followerRepository.unfollow("test-topic-id", "test-user-id") } returns true
        every { topicAggregateView.refreshView("test-topic-id") } returns mockk<TopicAggregate>()

        topicService.unfollow(topic, user)
        verify {
            followerRepository.unfollow("test-topic-id", "test-user-id")
            topicAggregateView.refreshView("test-topic-id")
        }

        // Test unsuccessful unfollow operation (not following)
        every { followerRepository.unfollow("test-topic-id", "test-user-id") } returns false

        topicService.unfollow(topic, user)
        verify(exactly = 1) { topicAggregateView.refreshView("test-topic-id") } // Should not be called again
    }
}
