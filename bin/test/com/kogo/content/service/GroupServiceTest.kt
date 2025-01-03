package com.kogo.content.service

import com.kogo.content.endpoint.model.GroupDto
import com.kogo.content.endpoint.model.GroupUpdate
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.search.index.GroupSearchIndex
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.repository.GroupRepository
import com.kogo.content.storage.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class GroupServiceTest {
    private val groupRepository: GroupRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val groupSearchIndex: GroupSearchIndex = mockk()

    private val groupService = GroupService(
        groupRepository = groupRepository,
        userRepository = userRepository,
        groupSearchIndex = groupSearchIndex
    )

    private lateinit var user: User
    private lateinit var group: Group

    @BeforeEach
    fun setup() {
        user = User(
            id = "test-user-id",
            username = "testuser",
            email = "test@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            )
        )

        group = Group(
            id = "test-group-id",
            groupName = "Test Group",
            description = "Test Description",
            owner = user,
            tags = mutableListOf("test", "group"),
            followerIds = mutableListOf(user.id!!),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    @Test
    fun `should find group by name`() {
        every { groupRepository.findByGroupName(group.groupName) } returns group

        val result = groupService.findByGroupName(group.groupName)

        assertThat(result).isEqualTo(group)
        verify { groupRepository.findByGroupName(group.groupName) }
    }

    @Test
    fun `should find groups by owner`() {
        val groups = listOf(group)
        every { groupRepository.findAllByOwnerId(user.id!!) } returns groups

        val result = groupService.findByOwner(user)

        assertThat(result).isEqualTo(groups)
        verify { groupRepository.findAllByOwnerId(user.id!!) }
    }

    @Test
    fun `should find groups by follower id`() {
        val groups = listOf(group)
        every { groupRepository.findAllByFollowerId(user.id!!) } returns groups

        val result = groupService.findAllByFollowerId(user.id!!)

        assertThat(result).isEqualTo(groups)
        verify { groupRepository.findAllByFollowerId(user.id!!) }
    }

    @Test
    fun `should search groups`() {
        val searchKeyword = "test"
        val paginationRequest = PaginationRequest(limit = 10)
        val expectedResult = PaginationSlice(items = listOf(group))

        every {
            groupSearchIndex.search(
                searchText = searchKeyword,
                paginationRequest = paginationRequest
            )
        } returns expectedResult

        val result = groupService.search(searchKeyword, paginationRequest)

        assertThat(result).isEqualTo(expectedResult)
        verify {
            groupSearchIndex.search(
                searchText = searchKeyword,
                paginationRequest = paginationRequest
            )
        }
    }

    @Test
    fun `should create group`() {
        val dto = GroupDto(
            groupName = "test-group",
            description = "test description",
            tags = listOf("tag1", "tag2"),
            profileImage = null
        )

        every { groupRepository.save(any()) } answers { firstArg() }

        val result = groupService.create(dto, user)

        assertThat(result.groupName).isEqualTo(dto.groupName)
        assertThat(result.description).isEqualTo(dto.description)
        assertThat(result.tags).isEqualTo(dto.tags)
        assertThat(result.owner).isEqualTo(user)
        verify { groupRepository.save(any()) }
    }

    @Test
    fun `should update group`() {
        val update = GroupUpdate(
            groupName = "updated-name",
            description = "updated description",
            tags = listOf("new-tag"),
            profileImage = null
        )
        val now = Instant.now()

        every { groupRepository.save(any()) } answers { firstArg() }

        val result = groupService.update(group, update)

        assertThat(result.groupName).isEqualTo(update.groupName)
        assertThat(result.description).isEqualTo(update.description)
        assertThat(result.tags).isEqualTo(update.tags)
        assertThat(result.updatedAt).isAfterOrEqualTo(now)
        verify { groupRepository.save(any()) }
    }

    @Test
    fun `should delete group`() {
        every { groupRepository.deleteById(group.id!!) } just Runs

        groupService.delete(group)

        verify { groupRepository.deleteById(group.id!!) }
    }

    @Test
    fun `should follow group successfully`() {
        val newUser = User(
            id = "new-user-id",
            username = "newuser",
            email = "newuser@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            )
        )

        every { groupRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(any()) } answers { firstArg() }

        val result = groupService.follow(group, newUser)

        assertThat(result).isTrue()
        assertThat(group.followerIds).contains(newUser.id)
        assertThat(newUser.followingGroupIds).contains(group.id)
        verify {
            groupRepository.save(group)
            userRepository.save(newUser)
        }
    }

    @Test
    fun `should not follow group if already following`() {
        val existingFollower = User(
            id = "existing-user-id",
            username = "existinguser",
            email = "existing@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            )
        )
        group.followerIds.add(existingFollower.id!!)

        val result = groupService.follow(group, existingFollower)

        assertThat(result).isFalse()
        verify(exactly = 0) {
            groupRepository.save(any())
            userRepository.save(any())
        }
    }

    @Test
    fun `should unfollow group successfully`() {
        val follower = User(
            id = "follower-id",
            username = "follower",
            email = "follower@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            )
        )
        group.followerIds.add(follower.id!!)
        follower.followingGroupIds.add(group.id!!)

        every { groupRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(any()) } answers { firstArg() }

        val result = groupService.unfollow(group, follower)

        assertThat(result).isTrue()
        assertThat(group.followerIds).doesNotContain(follower.id)
        assertThat(follower.followingGroupIds).doesNotContain(group.id)
        verify {
            groupRepository.save(group)
            userRepository.save(follower)
        }
    }

    @Test
    fun `should not unfollow group if not following`() {
        val nonFollower = User(
            id = "non-follower-id",
            username = "nonfollower",
            email = "nonfollower@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            )
        )

        val result = groupService.unfollow(group, nonFollower)

        assertThat(result).isFalse()
        verify(exactly = 0) {
            groupRepository.save(any())
            userRepository.save(any())
        }
    }

    @Test
    fun `should transfer group ownership`() {
        val newOwner = User(
            id = "new-owner-id",
            username = "newowner",
            email = "newowner@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            )
        )
        every { groupRepository.save(any()) } answers { firstArg() }

        val result = groupService.transferOwnership(group, newOwner)

        assertThat(result.owner).isEqualTo(newOwner)
        verify { groupRepository.save(group) }
    }
}
