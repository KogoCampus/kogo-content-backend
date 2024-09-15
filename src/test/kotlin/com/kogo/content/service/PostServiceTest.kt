package com.kogo.content.service

import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.PostRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostServiceTest {
    private val postRepository: PostRepository = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()
    private val fileHandler: FileHandler = mockk()

    private val postService: PostService = PostService(postRepository, attachmentRepository, fileHandler)

    @Test
    fun `should create a new post`() {
        val topic = mockk<Topic>()
        val author = mockk<UserDetails>()
        val postDto = mockk<PostDto>() {
            every { title } returns "post title"
            every { content } returns "post content"
            every { images } returns listOf(mockk())
            every { videos } returns listOf(mockk())
        }
        val attachment = mockk<Attachment>()
        every { attachmentRepository.saveFileAndReturnAttachment(any(), fileHandler, attachmentRepository) } returns attachment
        every { postRepository.save(any()) } returns mockk()

        postService.create(topic, author, postDto)

        verify {
            attachmentRepository.saveFileAndReturnAttachment(any(), fileHandler, attachmentRepository)
            postRepository.save(withArg {
                assertThat(it.title).isEqualTo("post title")
                assertThat(it.content).isEqualTo("post content")
                assertThat(it.author).isEqualTo(author)
                assertThat(it.topic).isEqualTo(topic)
                assertThat(it.attachments).containsExactly(attachment, attachment)
            })
        }
    }

    @Test
    fun `should update an existing post`() {
        val attachmentExisting1 = mockk<Attachment> { every { id } returns "attachment-id-1" }
        val attachmentExisting2 = mockk<Attachment> { every { id } returns "attachment-id-2" }
        val post = Post(
            id = "post-id",
            title = "post title",
            content = "post content",
            author = mockk<UserDetails>(),
            topic = mockk<Topic>(),
            attachments = listOf(attachmentExisting1, attachmentExisting2)
        )
        val postUpdate = PostUpdate(
            title = "updated post title",
            content = "post content",
            attachmentDelete = listOf("attachment-id-1"),
            images = listOf(mockk()),
            videos = listOf(mockk())
        )
        val attachmentToAdd = mockk<Attachment>()
        every { attachmentRepository.saveFileAndReturnAttachment(any(), fileHandler, attachmentRepository) } returns attachmentToAdd
        every { postRepository.save(any()) } returns post

        postService.update(post, postUpdate)

        verify {
            attachmentRepository.saveFileAndReturnAttachment(any(), fileHandler, attachmentRepository)
            postRepository.save(withArg {
                assertThat(it.title).isEqualTo(postUpdate.title)
                assertThat(it.content).isEqualTo(postUpdate.content)
                assertThat(it.attachments).containsExactly(attachmentExisting2, attachmentToAdd, attachmentToAdd)
            })
        }
    }
}
