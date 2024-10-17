package com.kogo.content.service

import com.kogo.content.endpoint.model.PaginationRequest
import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.filehandler.FileHandler
import com.kogo.content.searchengine.SearchIndexService
import com.kogo.content.storage.entity.Attachment
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import com.kogo.content.storage.repository.AttachmentRepository
import com.kogo.content.storage.repository.LikeRepository
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.storage.repository.ViewRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.security.SecurityProperties.User
import org.springframework.data.domain.PageRequest
import java.time.Instant

class PostServiceTest {
    private val postRepository: PostRepository = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()
    private val likeRepository: LikeRepository = mockk()
    private val viewRepository: ViewRepository = mockk()
    private val fileHandler: FileHandler = mockk()
    private val searchIndexService : SearchIndexService = mockk()

    private val postService: PostService = PostService(postRepository, attachmentRepository, likeRepository, viewRepository, fileHandler, searchIndexService)

    @Test
    fun `should return first page of posts in topic when page token param is not given`() {
        val createMockPost = { postId: String -> mockk<Post> { every { id } returns postId } }
        val topicId = "sample-topic-id"
        val paginationRequest = PaginationRequest(limit = 3, page = null)
        val postList = listOf(createMockPost("1"), createMockPost("2"), createMockPost("3"))
        every { postRepository.findAllByTopicId(topicId, any()) } returns postList
        val result = postService.listPostsByTopicId(topicId, paginationRequest)
        assertThat(result.items).isEqualTo(postList)
        assertThat(result.nextPage).isEqualTo(postList.lastOrNull()?.id)
        verify { postRepository.findAllByTopicId(topicId, any()) }
    }

    @Test
    fun `should return subsequent page of posts when page token is given`() {
        val createMockPost = { postId: String -> mockk<Post> { every { id } returns postId } }
        val topicId = "sample-topic-id"
        val paginationRequest = PaginationRequest(limit = 2, page = "somePageToken")
        val postList = listOf(createMockPost("1"), createMockPost("2"))
        every { postRepository.findAllByTopicIdAndIdLessThan(topicId, "somePageToken", any()) } returns postList
        val result = postService.listPostsByTopicId(topicId, paginationRequest)
        assertThat(result.items).isEqualTo(postList)
        assertThat(result.nextPage).isEqualTo(postList.lastOrNull()?.id)
        verify { postRepository.findAllByTopicIdAndIdLessThan(topicId, "somePageToken", any()) }
    }

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
                assertThat(it.owner).isEqualTo(author)
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
            owner = mockk<UserDetails>(),
            topic = mockk<Topic>(),
            attachments = listOf(attachmentExisting1, attachmentExisting2),
            comments = listOf(mockk()),
            createdAt = Instant.now()
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

    @Test
    fun `should create a like under the post`() {
        val postId = "post-id"
        val userId = "user-id"

        val author = mockk<UserDetails> {
            every { id } returns userId
        }

        every { likeRepository.save(any()) } returns mockk()
        every { postRepository.addLike(any()) } just Runs

        postService.addLike(postId, author)

        verify {
            likeRepository.save(withArg {
                assertThat(it.parentId).isEqualTo(postId)
                assertThat(it.userId).isEqualTo(userId)
            })
            postRepository.addLike(postId)
        }
    }

    @Test
    fun `should create a view under the post`() {
        val postId = "post-id"
        val userId = "user-id"

        val author = mockk<UserDetails> {
            every { id } returns userId
        }

        every { viewRepository.save(any()) } returns mockk()
        every { postRepository.addView(any()) } just Runs

        postService.addView(postId, author)

        verify {
            viewRepository.save(withArg {
                assertThat(it.parentId).isEqualTo(postId)
                assertThat(it.userId).isEqualTo(userId)
            })
            postRepository.addView(postId)
        }
    }
}
