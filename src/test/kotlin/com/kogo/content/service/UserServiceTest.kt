package com.kogo.content.service

import com.kogo.content.endpoint.model.UserUpdate
import com.kogo.content.service.fileuploader.FileUploaderService
import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import com.kogo.content.storage.repository.UserRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

class UserServiceTest {
    private val userRepository: UserRepository = mockk()
    private val fileService: FileUploaderService = mockk()

    private val userService = UserService(
        userRepository = userRepository,
        fileService = fileService
    )

    private lateinit var user: User
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
            ),
            profileImage = null
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

        // Mock security context for findCurrentUser
        val authentication: Authentication = mockk()
        val securityContext: SecurityContext = mockk()
        every { authentication.principal } returns user.username
        every { securityContext.authentication } returns authentication
        SecurityContextHolder.setContext(securityContext)
    }

    @Test
    fun `should find user by username`() {
        every { userRepository.findByUsername(user.username) } returns user

        val result = userService.findUserByUsername(user.username)

        assertThat(result).isEqualTo(user)
        verify { userRepository.findByUsername(user.username) }
    }

    @Test
    fun `should find user by email`() {
        every { userRepository.findByEmail(user.email) } returns user

        val result = userService.findUserByEmail(user.email)

        assertThat(result).isEqualTo(user)
        verify { userRepository.findByEmail(user.email) }
    }

    @Test
    fun `should find current user`() {
        every { userRepository.findByUsername(user.username) } returns user

        val result = userService.findCurrentUser()

        assertThat(result).isEqualTo(user)
        verify { userRepository.findByUsername(user.username) }
    }

    @Test
    fun `should create new user`() {
        val schoolInfo = SchoolInfo(
            schoolKey = "NEW",
            schoolName = "New School",
            schoolShortenedName = "NS"
        )

        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.create(
            username = "newuser",
            email = "newuser@example.com",
            schoolInfo = schoolInfo
        )

        assertThat(result.username).isEqualTo("newuser")
        assertThat(result.email).isEqualTo("newuser@example.com")
        assertThat(result.schoolInfo).isEqualTo(schoolInfo)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `should update username only`() {
        val update = UserUpdate(
            username = "updated-username"
        )

        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.update(user, update)

        assertThat(result.username).isEqualTo(update.username)
        assertThat(result.profileImage).isNull()
        verify { userRepository.save(any()) }
    }

    @Test
    fun `should update profile image only`() {
        val update = UserUpdate(
            profileImage = testImage
        )

        every { fileService.uploadImage(testImage) } returns testAttachment
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.update(user, update)

        assertThat(result.username).isEqualTo(user.username)
        assertThat(result.profileImage).isEqualTo(testAttachment)
        verify {
            fileService.uploadImage(testImage)
            userRepository.save(any())
        }
    }

    @Test
    fun `should update both username and profile image`() {
        val update = UserUpdate(
            username = "updated-username",
            profileImage = testImage
        )

        every { fileService.uploadImage(testImage) } returns testAttachment
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.update(user, update)

        assertThat(result.username).isEqualTo(update.username)
        assertThat(result.profileImage).isEqualTo(testAttachment)
        verify {
            fileService.uploadImage(testImage)
            userRepository.save(any())
        }
    }

    @Test
    fun `should update profile image and delete old one`() {
        // Set existing profile image
        user.profileImage = testAttachment

        val newImage = MockMultipartFile(
            "profileImage",
            "new-image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "new image content".toByteArray()
        )

        val newAttachment = testAttachment.copy(id = "new-image-id")
        val update = UserUpdate(
            profileImage = newImage
        )

        every { fileService.deleteImage(testAttachment.id) } just Runs
        every { fileService.uploadImage(newImage) } returns newAttachment
        every { userRepository.save(any()) } answers { firstArg() }

        val result = userService.update(user, update)

        assertThat(result.profileImage).isEqualTo(newAttachment)
        verify {
            fileService.deleteImage(testAttachment.id)
            fileService.uploadImage(newImage)
            userRepository.save(any())
        }
    }
}
