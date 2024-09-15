package com.kogo.content.service

import com.kogo.content.endpoint.model.TopicUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.TopicRepository
import com.kogo.content.util.fixture
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import kotlin.test.assertEquals

class TopicServiceTest {

    private val topicRepository : TopicRepository = mockk()

    private val attachmentRepository: AttachmentRepository = mockk()

    private val fileHandler: FileHandler = mockk()

    private val topicService: TopicService = TopicService(topicRepository, attachmentRepository, fileHandler)

    @BeforeEach
    fun setup() {
        clearMocks(topicRepository)
        clearMocks(attachmentRepository)
        clearMocks(fileHandler)
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
            fileName = "file-name",
            savedLocationURL = "saved-path",
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

    private fun createUserFixture() = fixture<UserDetails>()

    private fun createTopicFixture() = fixture<Topic> { mapOf(
        "owner" to createUserFixture()
    ) }
}
