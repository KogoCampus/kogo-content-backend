package com.kogo.content.service

import com.kogo.content.endpoint.model.GroupDto
import com.kogo.content.endpoint.model.GroupUpdate
import com.kogo.content.common.*
import com.kogo.content.endpoint.common.*
import com.kogo.content.search.SearchIndex
import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.entity.Follower
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.FollowerRepository
import com.kogo.content.storage.repository.GroupRepository
import com.kogo.content.storage.view.TopicAggregate
import com.kogo.content.storage.view.TopicAggregateView
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.time.Instant

class GroupServiceTest {

    private val groupRepository: GroupRepository = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()
    private val followerRepository: FollowerRepository = mockk()
    private val topicAggregateView: TopicAggregateView = mockk()
    private val topicAggregateSearchIndex: SearchIndex<TopicAggregate> = mockk()

    private val groupService = GroupService(
        groupRepository = groupRepository,
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
        val groupDto = GroupDto(
            groupName = "Test Topic",
            description = "Test Description",
            profileImage = profileImage,
            tags = listOf("tag1", "tag2")
        )
        val savedGroup = Group(
            id = "test-topic-id",
            groupName = groupDto.groupName,
            description = groupDto.description,
            owner = owner,
            profileImage = attachment,
            tags = groupDto.tags!!,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { attachmentRepository.saveFile(profileImage) } returns attachment
        every { groupRepository.save(any()) } returns savedGroup
        every { followerRepository.follow("test-topic-id", "test-owner-id") } returns mockk()
        every { topicAggregateView.refreshView("test-topic-id") } returns mockk()

        val result = groupService.create(groupDto, owner)

        assertThat(result).isEqualTo(savedGroup)
        verify {
            attachmentRepository.saveFile(profileImage)
            groupRepository.save(any())
            topicAggregateView.refreshView("test-topic-id")
        }
    }

    @Test
    fun `should update existing topic`() {
        val group = Group(
            id = "test-topic-id",
            groupName = "Original Name",
            description = "Original Description",
            owner = mockk(),
            createdAt = Instant.now().minusSeconds(3600),
            updatedAt = Instant.now().minusSeconds(1800)
        )
        val update = GroupUpdate(
            groupName = "Updated Name",
            description = "Updated Description",
            tags = listOf("new-tag"),
            profileImage = MockMultipartFile("image", "test.jpg", "image/jpeg", "test".toByteArray())
        )
        val attachment = mockk<Attachment>()

        every { attachmentRepository.saveFile(update.profileImage!!) } returns attachment
        every { groupRepository.save(any()) } returns group
        every { topicAggregateView.refreshView("test-topic-id") } returns mockk()

        val result = groupService.update(group, update)

        assertThat(result.groupName).isEqualTo(update.groupName)
        assertThat(result.description).isEqualTo(update.description)
        verify {
            attachmentRepository.saveFile(update.profileImage!!)
            groupRepository.save(any())
            topicAggregateView.refreshView("test-topic-id")
        }
    }

    @Test
    fun `should search topics by keyword`() {
        val searchText = "test"
        val paginationRequest = PaginationRequest(limit = 10)
        val topicAggregates = listOf(
            TopicAggregate(topicId = "topic-1", group = mockk(), followerCount = 10, postCount = 5),
            TopicAggregate(topicId = "topic-2", group = mockk(), followerCount = 20, postCount = 10)
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
                paginationRequest = paginationRequest
            )
        } returns paginationSlice

        val result = groupService.search(searchText, paginationRequest)

        assertThat(result.items).hasSize(2)
        verify { topicAggregateSearchIndex.search(searchText, paginationRequest) }
    }

    @Test
    fun `should handle follow operations correctly`() {
        val group = mockk<Group> { every { id } returns "test-topic-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test successful follow operation
        val follower = mockk<Follower>()
        every { followerRepository.follow("test-topic-id", "test-user-id") } returns follower
        every { topicAggregateView.refreshView("test-topic-id") } returns mockk<TopicAggregate>()

        groupService.follow(group, user)
        verify {
            followerRepository.follow("test-topic-id", "test-user-id")
            topicAggregateView.refreshView("test-topic-id")
        }

        // Test unsuccessful follow operation (already following)
        every { followerRepository.follow("test-topic-id", "test-user-id") } returns null

        groupService.follow(group, user)
        verify(exactly = 1) { topicAggregateView.refreshView("test-topic-id") } // Should not be called again
    }

    @Test
    fun `should handle unfollow operations correctly`() {
        val group = mockk<Group> { every { id } returns "test-topic-id" }
        val user = mockk<User> { every { id } returns "test-user-id" }

        // Test successful unfollow operation
        every { followerRepository.unfollow("test-topic-id", "test-user-id") } returns true
        every { topicAggregateView.refreshView("test-topic-id") } returns mockk<TopicAggregate>()

        groupService.unfollow(group, user)
        verify {
            followerRepository.unfollow("test-topic-id", "test-user-id")
            topicAggregateView.refreshView("test-topic-id")
        }

        // Test unsuccessful unfollow operation (not following)
        every { followerRepository.unfollow("test-topic-id", "test-user-id") } returns false

        groupService.unfollow(group, user)
        verify(exactly = 1) { topicAggregateView.refreshView("test-topic-id") } // Should not be called again
    }
}
