package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.service.*
import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.User
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {
    @MockkBean private lateinit var groupService: GroupService
    @MockkBean private lateinit var userService: UserService

    private lateinit var currentUser: User
    private lateinit var group: Group

    @BeforeEach
    fun setup() {
        currentUser = Fixture.createUserFixture()
        group = Fixture.createGroupFixture(owner = currentUser)

        every { userService.findCurrentUser() } returns currentUser
        every { groupService.findOrThrow(group.id!!) } returns group
        every { groupService.find(group.id!!) } returns group
    }

    @Test
    fun `should get group successfully`() {
        mockMvc.get("/media/groups/${group.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(group.id) }
            jsonPath("$.data.groupName") { value(group.groupName) }
            jsonPath("$.data.description") { value(group.description) }
            jsonPath("$.data.owner.id") { value(currentUser.id) }
            jsonPath("$.data.followerCount") { value(group.followers.size) }
            jsonPath("$.data.followedByCurrentUser") { value(group.isFollowing(currentUser)) }
        }
    }

    @Test
    fun `should create group successfully`() {
        val newGroup = Fixture.createGroupFixture(owner = currentUser)

        every { groupService.findByGroupName(newGroup.groupName) } returns null
        every { groupService.create(any(), currentUser) } returns newGroup
        every { groupService.follow(newGroup, currentUser) } returns true

        mockMvc.multipart("/media/groups") {
            part(MockPart("groupName", newGroup.groupName.toByteArray()))
            part(MockPart("description", newGroup.description.toByteArray()))
            file(MockMultipartFile("profileImage", "test.jpg", "image/jpeg", "test".toByteArray()))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.groupName") { value(newGroup.groupName) }
            jsonPath("$.data.description") { value(newGroup.description) }
            jsonPath("$.data.owner.id") { value(currentUser.id) }
            jsonPath("$.data.followerCount") { value(1) }
            jsonPath("$.data.followedByCurrentUser") { value(true) }
        }
    }

    @Test
    fun `should fail to create group with duplicate name`() {
        every { groupService.findByGroupName(group.groupName) } returns group

        mockMvc.multipart("/media/groups") {
            part(MockPart("groupName", group.groupName.toByteArray()))
            part(MockPart("description", "new description".toByteArray()))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value(ErrorCode.BAD_REQUEST.name) }
        }
    }

    @Test
    fun `should update group successfully when user is owner`() {
        val updatedName = "Updated Name"
        val updatedDescription = "Updated Description"
        val updatedGroup = group.copy().apply {
            groupName = updatedName
            description = updatedDescription
        }

        every { groupService.findByGroupName(updatedName) } returns null
        every { groupService.update(group, any()) } returns updatedGroup

        mockMvc.multipart("/media/groups/${group.id}") {
            part(MockPart("groupName", updatedName.toByteArray()))
            part(MockPart("description", updatedDescription.toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.groupName") { value(updatedName) }
            jsonPath("$.data.description") { value(updatedDescription) }
            jsonPath("$.data.owner.id") { value(currentUser.id) }
        }
    }

    @Test
    fun `should handle follow operations successfully`() {
        val followerUser = Fixture.createUserFixture()
        val groupToFollow = group.copy().apply {
            followers = mutableListOf() // ensure the group has no followers initially
        }

        every { groupService.find(group.id!!) } returns groupToFollow
        every { groupService.findOrThrow(group.id!!) } returns groupToFollow
        every { userService.findCurrentUser() } returns followerUser
        every { groupService.follow(groupToFollow, followerUser) } returns true

        mockMvc.put("/media/groups/${group.id}/follow") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.followerCount") { value(groupToFollow.followers.size) }
            jsonPath("$.data.followedByCurrentUser") { value(false) } // initially false since we just called follow
        }

        verify { groupService.follow(groupToFollow, followerUser) }
    }

    @Test
    fun `should handle error cases appropriately`() {
        // Non-existent group
        every { groupService.find("invalid-id") } returns null
        every { groupService.findOrThrow("invalid-id") } throws ResourceNotFoundException("Group", "invalid-id")

        mockMvc.get("/media/groups/invalid-id")
            .andExpect { status { isNotFound() } }

        // Unauthorized group update
        val differentUser = Fixture.createUserFixture()
        every { userService.findCurrentUser() } returns differentUser
        every { groupService.find(group.id!!) } returns group

        mockMvc.multipart("/media/groups/${group.id}") {
            part(MockPart("groupName", "new name".toByteArray()))
            with { it.method = "PUT"; it }
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
        }
    }
}
