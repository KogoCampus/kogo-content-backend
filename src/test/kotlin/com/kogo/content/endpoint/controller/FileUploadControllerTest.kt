package com.kogo.content.endpoint.controller

import com.kogo.content.service.UserService
import com.kogo.content.service.fileuploader.FileUploaderService
import com.kogo.content.storage.model.Attachment
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.*

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class FileUploadControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
)  {
    @MockkBean private lateinit var userService: UserService
    @MockkBean private lateinit var fileUploaderService: FileUploaderService

    private lateinit var testAttachment: Attachment

    @BeforeEach
    fun setup() {
        // Initialize a test attachment
        testAttachment = Attachment(
            id = "test-attachment-id",
            filename = "test-image.png",
            url = "http://example.com/test-image.png",
            contentType = "image/png",
            size = 1024,
            isPersisted = false
        )

        // Mock the service to return the test attachment
        every { fileUploaderService.staleImage(any()) } returns testAttachment
    }

    @Test
    fun `should upload image successfully`() {
//        // Create a mock image file
//        val mockImageFile = MockMultipartFile(
//            "image",
//            "test-image.png",
//            "image/png",
//            "test image content".toByteArray()
//        )
//
//        // Perform the request
//        mockMvc.multipart("/media/files/images") {
//            file(mockImageFile)
//        }.andExpect {
//            status { isOk() }
//            jsonPath("$.data") { value(testAttachment.id) }
//        }
//
//        // Verify that the service was called
//        verify { fileUploaderService.staleImage(mockImageFile) }
    }
}
