package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.service.filehandler.FileHandler
import com.kogo.content.filesystem.FileStoreKey
import com.kogo.content.service.entity.TopicService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.UserFollowing
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.FollowingTopicRepository
import com.kogo.content.storage.repository.TopicRepository
import com.kogo.content.util.fixture
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import kotlin.test.assertEquals

class TopicServiceTest {

    private val topicRepository : TopicRepository = mockk()

    private val attachmentRepository: AttachmentRepository = mockk()

    private val fileHandler: FileHandler = mockk()

    private val followingTopicRepository: FollowingTopicRepository = mockk()

    private val topicService: TopicService = TopicService(topicRepository, followingTopicRepository, attachmentRepository, fileHandler)

    @BeforeEach
    fun setup() {
        clearMocks(topicRepository)
        clearMocks(attachmentRepository)
        clearMocks(fileHandler)
    }

    @Test
    fun `should create a new topic`() {
        val owner = createUserFixture()
        val topicDto = mockk<TopicDto> {
            every { topicName } returns "new topic name"
            every { description } returns "new topic description"
            every { profileImage } returns mockk()
            every { tags } returns listOf("tag1", "tag2")
        }
        val profileAttachment = mockk<Attachment>()
        val savedTopic = createTopicFixture().copy(id = "topic-id", owner = owner)
        val userFollowing = UserFollowing(userId = owner.id!!, topicId = savedTopic.id!!).copy(id = "following-id")

        // Mock the save operations
        every { topicRepository.save(any()) } returns savedTopic
        every { followingTopicRepository.save(any()) } returns userFollowing
        every { attachmentRepository.saveFileAndReturnAttachment(any(), any(), any()) } returns profileAttachment

        // Call the service method
        val result = topicService.create(topicDto, owner)

        // Verify the save methods are called with the correct arguments
        verify {
            topicRepository.save(withArg {
                assertThat(it.topicName).isEqualTo("new topic name")
                assertThat(it.description).isEqualTo("new topic description")
                assertThat(it.owner).isEqualTo(owner)
                assertThat(it.profileImage).isEqualTo(profileAttachment)
                assertThat(it.tags).isEqualTo(listOf("tag1", "tag2"))
            })
            followingTopicRepository.save(withArg {
                assertThat(it.userId).isEqualTo(owner.id)
                assertThat(it.topicId).isEqualTo(savedTopic.id)
            })
        }

        // Assert that the result is correct
        assertEquals(savedTopic, result)
    }

    @Test
    fun `should update an existing topic`() {
        val topic = createTopicFixture()
        val topicUpdate = TopicUpdate(
            topicName = "updated topic name",
            description = "updated description",
            tags = listOf("tag1", "tag2"),
            profileImage = mockk()
        )
        val updatedAttachment = Attachment(
            id = "attachment-id",
            name = "file-name",
            storeKey = FileStoreKey("saved-path"),
            contentType = "image/png",
            fileSize = 1000
        )
        every { topicRepository.save((any())) } returns topic.copy(
            topicName = topicUpdate.topicName!!,
            description = topicUpdate.description!!,
            tags = topicUpdate.tags!!,
            profileImage = updatedAttachment
        )
        every { topicRepository.findByIdOrNull(topic.id!!) } returns topic
        every { attachmentRepository.saveFileAndReturnAttachment(any(), any(), any()) } returns updatedAttachment

        val updatedTopic = topicService.update(topic, topicUpdate)

        assertEquals(topicUpdate.topicName, updatedTopic.topicName)
        assertEquals(topicUpdate.description, updatedTopic.description)
        assertEquals(topicUpdate.tags, updatedTopic.tags)
        assertEquals(updatedAttachment, updatedTopic.profileImage)
    }

    @Test
    fun `should delete an existing topic`() {
        val profileImage = mockk<Attachment>()
        val topic = createTopicFixture().copy(id = "topic-id", profileImage = profileImage)

        // Mock the delete operations
        every { topicRepository.deleteById(topic.id!!) } just Runs
        every { attachmentRepository.delete(any()) } just Runs

        // Call the service method
        topicService.delete(topic)

        // Verify the delete operations
        verify {
            topicRepository.deleteById(topic.id!!)
            attachmentRepository.delete(profileImage) // No need for `!!` since profileImage is now non-null
        }
    }

    @Test
    fun `should follow a topic`() {
        val owner = createUserFixture()
        val topic = createTopicFixture().copy(userCount = 1)
        val userFollowing = UserFollowing(userId = owner.id!!, topicId = topic.id!!).copy(id = "following-id")

        // Mock the save operation to return a FollowingTopic object
        every { followingTopicRepository.save(any()) } returns userFollowing
        every { topicRepository.save(any<Topic>()) } returns topic.copy(userCount = topic.userCount + 1)

        // Call the service method
        topicService.follow(topic, owner)

        // Verify the save method is called with the correct arguments
        verify {
            followingTopicRepository.save(withArg {
                assertThat(it.userId).isEqualTo(owner.id)
                assertThat(it.topicId).isEqualTo(topic.id)
            })
            topicRepository.save(withArg {
                assertThat(it.userCount).isEqualTo(2) // Verify userCount is incremented
            })
        }
    }

    @Test
    fun `should unfollow a topic`() {
        val owner = createUserFixture()
        val topic = createTopicFixture().copy(userCount = 2)
        val userFollowing = UserFollowing(userId = owner.id!!, topicId = topic.id!!).copy(id = "following-id")

        every { followingTopicRepository.findByUserIdAndTopicId(owner.id!!, topic.id!!) } returns listOf(userFollowing)
        every { followingTopicRepository.deleteById(userFollowing.id!!) } just Runs
        every { topicRepository.save(any<Topic>()) } returns topic.copy(userCount = topic.userCount - 1)

        topicService.unfollow(topic, owner)

        verify {
            followingTopicRepository.deleteById(userFollowing.id!!)
            topicRepository.save(withArg {
                assertThat(it.userCount).isEqualTo(1) // Verify userCount is decremented
            })
        }
    }

    private fun createUserFixture() = fixture<UserDetails>()

    private fun createTopicFixture() = fixture<Topic> { mapOf(
        "owner" to createUserFixture()
    ) }
}
