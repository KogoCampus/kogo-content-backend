package com.kogo.content.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.kogo.content.endpoint.model.GroupUpdate
import com.kogo.content.endpoint.model.Enrollment
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.exception.FileOperationFailure
import com.kogo.content.exception.FileOperationFailureException
import com.kogo.content.search.index.GroupSearchIndex
import com.kogo.content.service.fileuploader.FileUploaderService
import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.entity.*
import com.kogo.content.storage.repository.GroupRepository
import com.kogo.content.storage.repository.UserRepository
import com.kogo.content.util.convertTo12BytesHexString
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile

class GroupServiceTest {
    private val groupRepository: GroupRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val groupSearchIndex: GroupSearchIndex = mockk()
    private val fileService: FileUploaderService = mockk()
    private val courseListingService: CourseListingService = mockk()
    private val userService: UserService = mockk()
    private val pushNotificationService: PushNotificationService = mockk()
    private val objectMapper = ObjectMapper()

    private val groupService = GroupService(
        groupRepository = groupRepository,
        userRepository = userRepository,
        groupSearchIndex = groupSearchIndex,
        fileService = fileService,
        courseListingService = courseListingService,
        pushNotificationService = pushNotificationService,
        userService = userService,
    )

    private lateinit var user: User
    private lateinit var group: Group
    private lateinit var testImage: MockMultipartFile
    private lateinit var testAttachment: Attachment

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

        testImage = MockMultipartFile(
            "profileImage",
            "test-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        testAttachment = Attachment(
            id = "test-image-id",
            filename = "test-image.jpg",
            contentType = MediaType.IMAGE_JPEG_VALUE,
            url = "test-url/test-image.jpg",
            size = 8000
        )

        group = Group(
            id = "test-group-id",
            groupName = "Test Group",
            description = "Test Description",
            owner = user,
            tags = mutableListOf("test", "group"),
            followers = mutableListOf(Follower(user)),
            profileImage = testAttachment,
            type = null,
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

    //@Test
    //fun `should create group with profileImage`() {
    //    val dto = GroupDto(
    //        groupName = "test-group",
    //        description = "test description",
    //        tags = listOf("tag1", "tag2"),
    //        profileImage = testImage
    //    )
    //
    //    every { fileService.uploadImage(testImage) } returns testAttachment
    //    every { groupRepository.save(any()) } answers { firstArg() }
    //
    //    val result = groupService.createUserGroup(dto, user)
    //
    //    assertThat(result.groupName).isEqualTo(dto.groupName)
    //    assertThat(result.description).isEqualTo(dto.description)
    //    assertThat(result.tags).isEqualTo(dto.tags)
    //    assertThat(result.owner).isEqualTo(user)
    //    assertThat(result.profileImage).isEqualTo(testAttachment)
    //
    //    verify {
    //        fileService.uploadImage(testImage)
    //        groupRepository.save(any())
    //    }
    //}

    @Test
    fun `should update group`() {
        val update = GroupUpdate(
            groupName = "updated-name",
            description = "updated description",
            tags = listOf("new-tag"),
            profileImage = null
        )
        val now = System.currentTimeMillis()

        every { groupRepository.save(any()) } answers { firstArg() }

        val result = groupService.update(group, update)

        assertThat(result.groupName).isEqualTo(update.groupName)
        assertThat(result.description).isEqualTo(update.description)
        assertThat(result.tags).isEqualTo(update.tags)
        assertThat(result.updatedAt).isGreaterThanOrEqualTo(now)
        verify { groupRepository.save(any()) }
    }

    @Test
    fun `should update group with new profile image and delete old one`() {
        val newImage = MockMultipartFile(
            "profileImage",
            "new-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "new image content".toByteArray()
        )

        val newAttachment = testAttachment.copy(id = "new-image-id")
        val update = GroupUpdate(
            groupName = "updated-name",
            description = "updated description",
            tags = listOf("new-tag"),
            profileImage = newImage
        )

        every { fileService.deleteFile(testAttachment.id) } just Runs
        every { fileService.uploadFile(newImage) } returns newAttachment
        every { groupRepository.save(any()) } answers { firstArg() }

        val result = groupService.update(group, update)

        assertThat(result.groupName).isEqualTo(update.groupName)
        assertThat(result.description).isEqualTo(update.description)
        assertThat(result.tags).isEqualTo(update.tags)
        assertThat(result.profileImage).isEqualTo(newAttachment)
        assertThat(result.updatedAt).isGreaterThanOrEqualTo(group.updatedAt)

        verify {
            fileService.deleteFile(testAttachment.id)
            fileService.uploadFile(newImage)
            groupRepository.save(any())
        }
    }

    @Test
    fun `should handle file operation failure during update gracefully`() {
        val newImage = MockMultipartFile(
            "profileImage",
            "new-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "new image content".toByteArray()
        )

        val newAttachment = testAttachment.copy(id = "new-image-id")
        val update = GroupUpdate(
            groupName = "updated-name",
            description = "updated description",
            tags = listOf("new-tag"),
            profileImage = newImage
        )

        every { fileService.deleteFile(testAttachment.id) } throws FileOperationFailureException(
            FileOperationFailure.DELETE,
            null,
            "Failed to delete"
        )
        every { fileService.uploadFile(newImage) } returns newAttachment
        every { groupRepository.save(any()) } answers { firstArg() }

        val result = groupService.update(group, update)

        assertThat(result.groupName).isEqualTo(update.groupName)
        assertThat(result.description).isEqualTo(update.description)
        assertThat(result.tags).isEqualTo(update.tags)
        assertThat(result.profileImage).isEqualTo(newAttachment)

        verify {
            fileService.deleteFile(testAttachment.id)
            fileService.uploadFile(newImage)
            groupRepository.save(any())
        }
    }

    @Test
    fun `should delete group and its profile image`() {
        every { fileService.deleteFile(testAttachment.id) } just Runs
        every { groupRepository.deleteById(group.id!!) } just Runs

        groupService.delete(group)

        verify {
            fileService.deleteFile(testAttachment.id)
            groupRepository.deleteById(group.id!!)
        }
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
        assertThat(group.followers.any { it.follower.id == newUser.id}).isEqualTo(true)
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
        group.followers.add(Follower(existingFollower))

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
        group.followers.add(Follower(follower))
        follower.followingGroupIds.add(group.id!!)

        every { groupRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(any()) } answers { firstArg() }

        val result = groupService.unfollow(group, follower)

        assertThat(result).isTrue()
        assertThat(group.followers.any { it.follower.id == follower.id }).isEqualTo(false)
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

    @Test
    fun `should update course enrollment successfully`() {
        val courseCodeBase64 = java.util.Base64.getEncoder().encodeToString("CS101".toByteArray())
        val enrollment = Enrollment(
            schoolKey = "test_school",
            base64CourseCodes = listOf(courseCodeBase64)
        )

        val systemUser = User(
            id = "system-user-id",
            username = "system",
            email = "system@kogo.com",
            schoolInfo = SchoolInfo(
                schoolKey = "SYSTEM",
                schoolName = "System",
                schoolShortenedName = null
            )
        )

        val courseListing = CourseListingService.CourseListing(
            semester = "Fall 2023",
            programs = objectMapper.readTree("""
                [{
                    "courses": [{
                        "courseCode": "CS101",
                        "courseName": "Introduction to Computer Science"
                    }]
                }]
            """),
            schoolKey = "test_school"
        )

        val courseGroup = Group(
            id = convertTo12BytesHexString(courseCodeBase64),
            groupName = "CS101",
            description = "Introduction to Computer Science",
            owner = systemUser,
            type = GroupType.COURSE_GROUP,
            tags = mutableListOf()
        )

        // Mock existing course group that should be dropped
        val oldCourseGroup = Group(
            id = "old-course-id",
            groupName = "OLD101",
            description = "Old Course",
            owner = systemUser,
            type = GroupType.COURSE_GROUP,
            followers = mutableListOf(Follower(user))
        )
        user.followingGroupIds.add(oldCourseGroup.id!!)

        every { courseListingService.retrieveCourseListing(enrollment.schoolKey) } returns courseListing
        every { groupRepository.findByIdOrNull(courseGroup.id!!) } returns null
        every { groupRepository.findByIdOrNull(oldCourseGroup.id!!) } returns oldCourseGroup
        every { groupRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(any()) } answers { firstArg() }

        val result = groupService.updateCourseEnrollment(user, enrollment)

        // Verify the result
        assertThat(result).hasSize(1)
        assertThat(result[0].groupName).isEqualTo("CS101")
        assertThat(result[0].description).isEqualTo("Introduction to Computer Science")
        assertThat(result[0].type).isEqualTo(GroupType.COURSE_GROUP)
        assertThat(result[0].isFollowedBy(user)).isTrue()

        // Verify that old course group is properly unfollowed
        assertThat(user.followingGroupIds)
            .withFailMessage("User should not be following old course group anymore")
            .doesNotContain(oldCourseGroup.id)

        verify {
            groupRepository.save(any())
            userRepository.save(user)
            courseListingService.retrieveCourseListing(enrollment.schoolKey)
        }
    }

    @Test
    fun `should handle empty course codes in enrollment`() {
        val enrollment = Enrollment(
            schoolKey = "test_school",
            base64CourseCodes = emptyList()
        )

        val courseListing = CourseListingService.CourseListing(
            semester = "Fall 2023",
            programs = objectMapper.readTree("[]"),
            schoolKey = "test_school"
        )

        every { courseListingService.retrieveCourseListing(enrollment.schoolKey) } returns courseListing

        val result = groupService.updateCourseEnrollment(user, enrollment)

        assertThat(result).isEmpty()
        verify { courseListingService.retrieveCourseListing(enrollment.schoolKey) }
    }
}
