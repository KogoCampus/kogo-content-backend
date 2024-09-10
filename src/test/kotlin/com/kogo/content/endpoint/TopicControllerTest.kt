package com.kogo.content.endpoint

import com.kogo.content.endpoint.model.TopicDto
import com.kogo.content.service.TopicService
import com.kogo.content.storage.entity.TopicEntity
import com.kogo.content.util.fixture
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest
class TopicControllerTest @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @MockkBean
    lateinit var topicService: TopicService

    @Nested
    inner class `when get request to groups`() {
        @Test
        fun `should retrieve an existing group by group id`() {
            val serviceResponse = fixture<TopicEntity>()
            every { topicService.find("1") } returns serviceResponse
            mockMvc.get("/media/groups/1")
                .andExpect { status { isOk() } }
                .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
                .andExpect { jsonPath("$.data.id") { value(serviceResponse.id) } }
        }
    }

    @Nested
    inner class `when post request to groups`() {
        @Test
        fun `should create a group`() {
            val mockProfileImage = MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray())
            val group = fixture<TopicDto> { mapOf(
                "groupName" to "dummy group name",
                "description" to "description",
                "tags" to "tag1,tag2,tag3",
                "profileImage" to mockProfileImage
            ) }
            every { topicService.create(any()) } returns group.toEntity()
            mockMvc.perform(
                multipart("/media/groups")
                    .part(MockPart("groupName", group.groupName.toByteArray()))
                    .part(MockPart("description", group.description.toByteArray()))
                    .part(MockPart("tags", group.tags.toByteArray()))
                    .file(mockProfileImage)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .with { it.method = "POST"; it })
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.groupName").value(group.groupName))
                .andExpect(jsonPath("$.data.description").value(group.description))
                .andExpect(jsonPath("$.data.tags").isArray)
                .andExpect(jsonPath("$.data.tags.length()").value(3))
        }

        @Test
        fun `should reject if payload is empty`() {
            mockMvc.perform(
                multipart("/media/groups")
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        }

        @Test
        fun `should reject if groupName is empty`() {
            mockMvc.perform(
                multipart("/media/groups")
                    .part(MockPart("description", "description".toByteArray()))
                    .part(MockPart("tags", "tag1,tag2".toByteArray()))
                    .file(MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray()))
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        }
    }

    @Nested
    inner class `when put request to groups`() {
        @Test
        fun `should update a group`() {
            val mockProfileImage = MockMultipartFile("profileImage", "image.jpeg", "image/jpeg", "some image".toByteArray())
            val group = fixture<TopicDto> { mapOf(
                "groupName" to "dummy group name",
                "description" to "description",
                "profileImage" to mockProfileImage
            ) }
            every { topicService.update("1", any()) } returns group.toEntity()
            mockMvc.perform(
                multipart("/media/groups/1")
                    .part(MockPart("groupName", group.groupName.toByteArray()))
                    .part(MockPart("description", group.description.toByteArray()))
                    .file(mockProfileImage)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .with { it.method = "PUT"; it })
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.groupName").value(group.groupName))
                .andExpect(jsonPath("$.data.description").value(group.description))
        }

        @Test
        fun `should reject if payload is empty`() {
            mockMvc.perform(
                multipart("/media/groups/1")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .with { it.method = "PUT"; it })
                .andExpect(status().isBadRequest)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        }
    }

    @Nested
    inner class `when delete request groups`() {
        @Test
        fun `should delete a group`() {
            every { topicService.delete("1") } returns Unit
            mockMvc.delete("/media/groups/1")
                .andExpect { status { isOk() } }
                .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
        }
    }
}
