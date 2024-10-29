package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.service.search.SearchService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.entity.UserFollowing
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.TopicRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.mock.web.MockMultipartFile

class TopicServiceTest {

    private val topicRepository: TopicRepository = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()
    private val fileHandler: FileHandler = mockk()
    private val topicSearchService: SearchService<Topic> = mockk()

    private val topicService: TopicService = TopicService(
        topicRepository = topicRepository,
        attachmentRepository = attachmentRepository,
        fileHandler = fileHandler,
        topicSearchService = topicSearchService)

    @BeforeEach
    fun setup() {
        clearMocks(topicRepository)
        clearMocks(attachmentRepository)
        clearMocks(fileHandler)
    }

    @Test
    fun `should find topic by id`() {
        val topicId = "test-topic-id"
        val expectedTopic = mockk<Topic>()

        every { topicRepository.findByIdOrNull(topicId) } returns expectedTopic

        val result = topicService.find(topicId)

        assertThat(result).isEqualTo(expectedTopic)
        verify { topicRepository.findByIdOrNull(topicId) }
    }

    @Test
    fun `should list following topics by user id`() {
        val userId = "test-user-id"
        val following1 = mockk<UserFollowing> { every { followableId } returns "topic-1" }
        val following2 = mockk<UserFollowing> { every { followableId } returns "topic-2" }
        val topic1 = mockk<Topic>()
        val topic2 = mockk<Topic>()

        every { topicRepository.findAllFollowingsByUserId(userId) } returns listOf(following1, following2)
        every { topicRepository.findByIdOrNull("topic-1") } returns topic1
        every { topicRepository.findByIdOrNull("topic-2") } returns topic2

        val result = topicService.listFollowingTopicsByUserId(userId)

        assertThat(result).containsExactly(topic1, topic2)
        verify { topicRepository.findAllFollowingsByUserId(userId) }
    }

    @Test
    fun `should create a new topic`() {
        val owner = mockk<UserDetails> { every { id } returns "test-owner-user-id" }
        val profileImage = MockMultipartFile("image", "test.jpg", "image/jpeg", "test".toByteArray())
        val attachment = mockk<Attachment>()
        val topicDto = TopicDto(
            topicName = "Test Topic",
            description = "Test Description",
            profileImage = profileImage,
            tags = listOf("tag1", "tag2")
        )

        val expectedTopic = Topic(
            id = "test-topic-id",
            topicName = topicDto.topicName,
            description = topicDto.description,
            owner = owner,
            profileImage = attachment,
            tags = topicDto.tags!!
        )

        every { attachmentRepository.saveFileAndReturnAttachment(profileImage, fileHandler, attachmentRepository) } returns attachment
        every { topicRepository.save(any()) } returns expectedTopic
        every { topicRepository.follow("test-topic-id", "test-owner-user-id") } returns mockk()

        val result = topicService.create(topicDto, owner)

        assertThat(result).isEqualTo(expectedTopic)
        verify {
            attachmentRepository.saveFileAndReturnAttachment(profileImage, fileHandler, attachmentRepository)
            topicRepository.save(any())
            topicRepository.follow("test-topic-id", "test-owner-user-id")
        }
    }

    @Test
    fun `should update an existing topic`() {
        val topic = Topic(
            id = "test-topic-id",
            topicName = "Old Name",
            description = "Old Description",
            owner = mockk(),
            tags = listOf("old-tag")
        )
        val profileImage = MockMultipartFile("image", "test.jpg", "image/jpeg", "test".toByteArray())
        val newAttachment = mockk<Attachment>()
        val topicUpdate = TopicUpdate(
            topicName = "New Name",
            description = "New Description",
            profileImage = profileImage,
            tags = listOf("new-tag")
        )

        every { attachmentRepository.saveFileAndReturnAttachment(profileImage, fileHandler, attachmentRepository) } returns newAttachment
        every { topicRepository.save(any()) } returns topic

        val result = topicService.update(topic, topicUpdate)

        assertThat(result.topicName).isEqualTo("New Name")
        assertThat(result.description).isEqualTo("New Description")
        assertThat(result.tags).containsExactly("new-tag")
        assertThat(result.profileImage).isEqualTo(newAttachment)
        verify {
            attachmentRepository.saveFileAndReturnAttachment(profileImage, fileHandler, attachmentRepository)
            topicRepository.save(topic)
        }
    }

    @Test
    fun `should delete an existing topic`() {
        val topic = Topic(
            id = "test-topic-id",
            topicName = "Test Topic",
            owner = mockk(),
            profileImage = mockk()
        )

        every { attachmentRepository.delete(any()) } just Runs
        every { topicRepository.unfollowAllByFollowableId(topic.id!!) } just Runs
        every { topicRepository.deleteById(topic.id!!) } just Runs

        topicService.delete(topic)

        verify {
            attachmentRepository.delete(topic.profileImage!!)
            topicRepository.unfollowAllByFollowableId(topic.id!!)
            topicRepository.deleteById(topic.id!!)
        }
    }

    @Test
    fun `should follow a topic`() {
        val user = mockk<UserDetails> { every { id } returns "test-user-id" }
        val topic = Topic(
            id = "test-topic-id",
            topicName = "Test Topic",
            owner = mockk(),
            followerCount = 0
        )

        every { topicRepository.follow("test-topic-id", "test-user-id") } returns mockk()
        every { topicRepository.save(any()) } returns topic

        val result = topicService.follow(topic, user)

        assertThat(result.followerCount).isEqualTo(1)
        verify {
            topicRepository.follow("test-topic-id", "test-user-id")
            topicRepository.save(topic)
        }
    }

    @Test
    fun `should unfollow a topic`() {
        val user = mockk<UserDetails> { every { id } returns "test-user-id" }
        val topic = Topic(
            id = "test-topic-id",
            topicName = "Test Topic",
            owner = mockk(),
            followerCount = 1
        )

        every { topicRepository.unfollow("test-topic-id", "test-user-id") } returns true
        every { topicRepository.save(any()) } returns topic

        val result = topicService.unfollow(topic, user)

        assertThat(result.followerCount).isEqualTo(0)
        verify {
            topicRepository.unfollow("test-topic-id", "test-user-id")
            topicRepository.save(topic)
        }
    }

    @Test
    fun `should transfer ownership`() {
        val newOwner = mockk<UserDetails>()
        val topic = Topic(
            id = "test-topic-id",
            topicName = "Test Topic",
            owner = mockk()
        )

        every { topicRepository.save(any()) } returns topic

        val result = topicService.transferOwnership(topic, newOwner)

        assertThat(result.owner).isEqualTo(newOwner)
        verify { topicRepository.save(topic) }
    }
}
