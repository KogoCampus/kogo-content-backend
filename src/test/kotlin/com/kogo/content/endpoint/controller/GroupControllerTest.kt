package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.service.*
import com.kogo.test.util.Fixture
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.model.entity.Follower
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
            status { isConflict() }
            jsonPath("$.error") { value(ErrorCode.DUPLICATED.name) }
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
    fun `should fail to unfollow if the owner`() {
        val owner = group.owner
        every { userService.findCurrentUser() } returns owner
        every { groupService.find(group.id!!) } returns group

        mockMvc.put("/media/groups/${group.id}/unfollow") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value(ErrorCode.BAD_REQUEST.name) }
            jsonPath("$.details") { value("The owner cannot unfollow the group") }
        }

        verify(exactly = 0) { groupService.unfollow(any(), any()) }
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

    @Test
    fun `should delete group successfully when user is owner`() {
        every { groupService.delete(group) } returns Unit

        mockMvc.delete("/media/groups/${group.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }

        verify { groupService.delete(group) }
    }

    @Test
    fun `should fail to delete group when user is not owner`() {
        val differentUser = Fixture.createUserFixture()
        val groupOwnedByOther = Fixture.createGroupFixture(owner = differentUser)

        every { userService.findCurrentUser() } returns currentUser
        every { groupService.find(groupOwnedByOther.id!!) } returns groupOwnedByOther
        every { groupService.findOrThrow(groupOwnedByOther.id!!) } returns groupOwnedByOther

        mockMvc.delete("/media/groups/${groupOwnedByOther.id}") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
            jsonPath("$.details") { value("group is not owned by user ${currentUser.id}") }
        }

        verify(exactly = 0) { groupService.delete(any()) }
    }

    @Test
    fun `should fail to follow group when already following`() {
        val followerUser = Fixture.createUserFixture()
        val groupToFollow = Fixture.createGroupFixture(owner = currentUser)
        groupToFollow.followers.add(Follower(followerUser))

        every { groupService.find(groupToFollow.id!!) } returns groupToFollow
        every { groupService.findOrThrow(groupToFollow.id!!) } returns groupToFollow
        every { userService.findCurrentUser() } returns followerUser

        mockMvc.put("/media/groups/${groupToFollow.id}/follow") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value(ErrorCode.BAD_REQUEST.name) }
            jsonPath("$.details") { value("The user is already following the group") }
        }

        verify(exactly = 0) { groupService.follow(any(), any()) }
    }

    @Test
    fun `should unfollow group successfully when user is follower`() {
        val followerUser = Fixture.createUserFixture()
        val groupToUnfollow = Fixture.createGroupFixture(owner = currentUser)
        groupToUnfollow.followers.add(Follower(followerUser))

        every { groupService.find(groupToUnfollow.id!!) } returns groupToUnfollow
        every { groupService.findOrThrow(groupToUnfollow.id!!) } returns groupToUnfollow
        every { userService.findCurrentUser() } returns followerUser
        every { groupService.unfollow(groupToUnfollow, followerUser) } returns true

        mockMvc.put("/media/groups/${groupToUnfollow.id}/unfollow") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.id") { value(groupToUnfollow.id) }
        }

        verify { groupService.unfollow(groupToUnfollow, followerUser) }
    }

    @Test
    fun `should fail to unfollow group when not following`() {
        val nonFollowerUser = Fixture.createUserFixture()
        val groupToUnfollow = Fixture.createGroupFixture(owner = currentUser)

        every { groupService.find(groupToUnfollow.id!!) } returns groupToUnfollow
        every { groupService.findOrThrow(groupToUnfollow.id!!) } returns groupToUnfollow
        every { userService.findCurrentUser() } returns nonFollowerUser

        mockMvc.put("/media/groups/${groupToUnfollow.id}/unfollow") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value(ErrorCode.BAD_REQUEST.name) }
            jsonPath("$.details") { value("The user is not following the group") }
        }

        verify(exactly = 0) { groupService.unfollow(any(), any()) }
    }

    @Test
    fun `should delete group profile image successfully when user is owner`() {
        val updatedGroup = group.copy().apply {
            profileImage = null
        }

        every { groupService.deleteProfileImage(group) } returns updatedGroup

        mockMvc.delete("/media/groups/${group.id}/profileImage") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.profileImage") { doesNotExist() }
        }

        verify { groupService.deleteProfileImage(group) }
    }

    @Test
    fun `should fail to delete group profile image when user is not owner`() {
        val differentUser = Fixture.createUserFixture()
        val groupOwnedByOther = Fixture.createGroupFixture(owner = differentUser)

        every { userService.findCurrentUser() } returns currentUser
        every { groupService.find(groupOwnedByOther.id!!) } returns groupOwnedByOther
        every { groupService.findOrThrow(groupOwnedByOther.id!!) } returns groupOwnedByOther

        mockMvc.delete("/media/groups/${groupOwnedByOther.id}/profileImage") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
            jsonPath("$.details") { value("group is not owned by user ${currentUser.id}") }
        }

        verify(exactly = 0) { groupService.deleteProfileImage(any()) }
    }

    @Test
    fun `should get group followers when user is owner`() {
        val followerUser = Fixture.createUserFixture()
        val paginationSlice = PaginationSlice(
            items = listOf(followerUser),
            nextPageToken = null
        )

        every { userService.findAllFollowersByGroup(group, any()) } returns paginationSlice

        mockMvc.get("/media/groups/${group.id}/followers") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(followerUser.id) }
            jsonPath("$.data[0].username") { value(followerUser.username) }
        }

        verify { userService.findAllFollowersByGroup(group, any()) }
    }

    @Test
    fun `should fail to get group followers when user is not owner`() {
        val differentUser = Fixture.createUserFixture()
        val groupOwnedByOther = Fixture.createGroupFixture(owner = differentUser)

        every { userService.findCurrentUser() } returns currentUser
        every { groupService.findOrThrow(groupOwnedByOther.id!!) } returns groupOwnedByOther

        mockMvc.get("/media/groups/${groupOwnedByOther.id}/followers") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.error") { value(ErrorCode.USER_ACTION_DENIED.name) }
            jsonPath("$.details") { value("group is not owned by user") }
        }

        verify(exactly = 0) { userService.findAllFollowersByGroup(any(), any()) }
    }
}
