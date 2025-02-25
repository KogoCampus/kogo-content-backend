package com.kogo.content.service

import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.exception.FileOperationFailure
import com.kogo.content.exception.FileOperationFailureException
import com.kogo.content.search.SearchIndex
import com.kogo.content.service.fileuploader.FileUploaderService
import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.Comment
import com.kogo.content.storage.model.Like
import com.kogo.content.storage.model.Reply
import com.kogo.content.storage.model.entity.*
import com.kogo.content.storage.pagination.MongoPaginationQueryBuilder
import com.kogo.content.storage.repository.PostRepository
import com.kogo.content.util.convertTo12BytesHexString
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import java.util.concurrent.CompletableFuture

class PostServiceTest {
    private val postRepository: PostRepository = mockk()
    private val postSearchIndex: SearchIndex<Post> = mockk()
    private val fileService: FileUploaderService = mockk()
    private val pushNotificationService: PushNotificationService = mockk()
    private val mongoPaginationQueryBuilder: MongoPaginationQueryBuilder = mockk()

    private val postService = PostService(
        postRepository = postRepository,
        postSearchIndex = postSearchIndex,
        fileService = fileService,
        pushNotificationService = pushNotificationService
    ).apply {
        mongoPaginationQueryBuilder = this@PostServiceTest.mongoPaginationQueryBuilder
    }

    private lateinit var user: User
    private lateinit var group: Group
    private lateinit var post: Post
    private lateinit var testImage: MockMultipartFile
    private lateinit var testAttachment: Attachment
    private lateinit var testFileToken: String

    @BeforeEach
    fun setup() {
        user = User(
            id = "test-user-id",
            username = "testuser",
            email = "test@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolGroupId = "test-school-group-id",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            )
        )

        group = Group(
            id = convertTo12BytesHexString("test-group-id"),
            groupName = "Test Group",
            description = "Test Description",
            type = null,
            owner = user,
            tags = mutableListOf("test", "group"),
            followers = mutableListOf(Follower(user)),
        )

        testImage = MockMultipartFile(
            "test-image",
            "test.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image content".toByteArray()
        )

        testAttachment = Attachment(
            id = "test-attachment-id",
            filename = "test.jpg",
            url = "http://test-url/test.jpg",
            contentType = MediaType.IMAGE_JPEG_VALUE,
            size = 1024L
        )

        testFileToken = "test-file-token"

        post = Post(
            id = "test-post-id",
            title = "Test Post",
            content = "Test Content",
            group = group,
            author = user,
            attachments = mutableListOf(testAttachment),
            comments = mutableListOf(),
            likes = mutableListOf(),
            viewerIds = mutableListOf(),
        )
    }

    @Test
    fun `should create new post`() {
        val postDto = PostDto(
            title = "Test Post",
            content = "Test Content",
        )

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.create(group, user, postDto)

        assertThat(result.title).isEqualTo(postDto.title)
        assertThat(result.content).isEqualTo(postDto.content)
        assertThat(result.group).isEqualTo(group)
        assertThat(result.author).isEqualTo(user)
        verify {
            postRepository.save(any())
        }
    }

    @Test
    fun `should update post`() {
        val postUpdate = PostUpdate(
            title = "Updated Title",
            content = "Updated Content",
            attachmentDeleteIds = emptyList()
        )

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.update(post, postUpdate)

        assertThat(result.title).isEqualTo(postUpdate.title)
        assertThat(result.content).isEqualTo(postUpdate.content)
        assertThat(result.updatedAt).isGreaterThanOrEqualTo(post.updatedAt)
        verify { postRepository.save(any()) }
    }

    @Test
    fun `should delete post`() {
        every { fileService.deleteFile(testAttachment.id) } just Runs
        every { postRepository.deleteById(post.id!!) } just Runs

        postService.delete(post)

        verify {
            fileService.deleteFile(testAttachment.id)
            postRepository.deleteById(post.id!!)
        }
    }

    @Test
    fun `should add comment to post`() {
        val commentContent = "Test Comment"

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.addCommentToPost(post, commentContent, user)

        assertThat(result.content).isEqualTo(commentContent)
        assertThat(result.author).isEqualTo(user)
        assertThat(post.comments).contains(result)

        verify {
            postRepository.save(post)
        }
    }

    @Test
    fun `should remove comment from post`() {
        val comment = Comment(
            id = ObjectId().toString(),
            content = "Test Comment",
            author = user
        )
        post.comments.add(comment)

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.removeCommentFromPost(post, comment.id.toString())

        assertThat(result).isTrue()
        assertThat(post.comments).doesNotContain(comment)
        verify { postRepository.save(post) }
    }

    @Test
    fun `should add reply to comment`() {
        val comment = Comment(
            id = ObjectId().toString(),
            content = "Test Comment",
            author = user
        )
        post.comments.add(comment)
        val replyContent = "Test Reply"

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.addReplyToComment(post, comment.id, replyContent, user)

        assertThat(result.content).isEqualTo(replyContent)
        assertThat(result.author).isEqualTo(user)
        assertThat(comment.replies).contains(result)

        verify {
            postRepository.save(post)
        }
    }

    @Test
    fun `should add like to post`() {
        every { postRepository.addLikeToPost(post, user.id!!) } returns true

        val result = postService.addLikeToPost(post, user)

        assertThat(result).isTrue()
        verify { postRepository.addLikeToPost(post, user.id!!) }
    }

    @Test
    fun `should not add duplicate active like to post`() {
        post.likes.add(Like(userId = user.id!!, isActive = true))

        every { postRepository.addLikeToPost(post, user.id!!) } returns false

        val result = postService.addLikeToPost(post, user)

        assertThat(result).isFalse()
        verify { postRepository.addLikeToPost(post, user.id!!) }
        verify(exactly = 0) { pushNotificationService.sendPushNotification(any()) }
    }

    @Test
    fun `should reactivate inactive like on post`() {
        post.likes.add(Like(userId = user.id!!, isActive = false))

        every { postRepository.addLikeToPost(post, user.id!!) } returns true

        val result = postService.addLikeToPost(post, user)

        assertThat(result).isTrue()
        verify { postRepository.addLikeToPost(post, user.id!!) }
    }

    @Test
    fun `should add viewer to post`() {
        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.addViewer(post, user)

        assertThat(result).isTrue()
        assertThat(post.viewerIds).contains(user.id)
        verify { postRepository.save(post) }
    }

    @Test
    fun `should not add duplicate viewer to post`() {
        post.viewerIds.add(user.id!!)

        val result = postService.addViewer(post, user)

        assertThat(result).isFalse()
        assertThat(post.viewerIds).hasSize(1)
        verify(exactly = 0) { postRepository.save(any()) }
    }

    @Test
    fun `should search posts in group`() {
        val searchKeyword = "test"
        val paginationRequest = PaginationRequest(limit = 10)
        val expectedResult = PaginationSlice(items = listOf(post))

        every {
            postSearchIndex.search(
                searchText = searchKeyword,
                paginationRequest = paginationRequest.withFilter("group", ObjectId(group.id!!))
            )
        } returns expectedResult

        val result = postService.searchPostsInGroup(searchKeyword, group.id!!, paginationRequest)

        assertThat(result).isEqualTo(expectedResult)
        verify {
            postSearchIndex.search(
                searchText = searchKeyword,
                paginationRequest = paginationRequest.withFilter("group", ObjectId(group.id!!))
            )
        }
    }

    @Test
    fun `should create new post with images`() {
        val postDto = PostDto(
            title = "Test Post",
            content = "Test Content",
            fileTokens = listOf(testFileToken),
        )

        every { fileService.decodeFileToken(testFileToken) } returns testAttachment.id
        every { fileService.persistFile(testAttachment.id) } returns testAttachment
        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.create(group, user, postDto)

        assertThat(result.title).isEqualTo(postDto.title)
        assertThat(result.content).isEqualTo(postDto.content)
        assertThat(result.group).isEqualTo(group)
        assertThat(result.author).isEqualTo(user)
        assertThat(result.attachments).hasSize(1)
        assertThat(result.attachments[0]).isEqualTo(testAttachment)

        verify {
            fileService.decodeFileToken(testFileToken)
            fileService.persistFile(testAttachment.id)
            postRepository.save(any())
        }
    }

    @Test
    fun `should update post with new images and delete old ones`() {
        val newImage = MockMultipartFile(
            "new-image",
            "new.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "new image content".toByteArray()
        )

        val newAttachment = testAttachment.copy(id = "new-attachment-id")

        val postUpdate = PostUpdate(
            title = "Updated Title",
            content = "Updated Content",
            images = listOf(newImage),
            fileTokens = listOf(testFileToken),
            attachmentDeleteIds = listOf(testAttachment.id)
        )

        every { fileService.decodeFileToken(testFileToken) } returns newAttachment.id
        every { fileService.persistFile(newAttachment.id) } returns newAttachment
        every { fileService.deleteFile(testAttachment.id) } just Runs
        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.update(post, postUpdate)

        assertThat(result.title).isEqualTo(postUpdate.title)
        assertThat(result.content).isEqualTo(postUpdate.content)
        assertThat(result.attachments).hasSize(1)
        assertThat(result.attachments[0]).isEqualTo(newAttachment)
        assertThat(result.updatedAt).isGreaterThanOrEqualTo(post.updatedAt)

        verify {
            fileService.decodeFileToken(testFileToken)
            fileService.persistFile(newAttachment.id)
            fileService.deleteFile(testAttachment.id)
            postRepository.save(any())
        }
    }

    @Test
    fun `should handle file operation failure during update gracefully`() {
        val postUpdate = PostUpdate(
            title = "Updated Title",
            content = "Updated Content",
            attachmentDeleteIds = listOf(testAttachment.id)
        )

        every { fileService.deleteFile(testAttachment.id) } throws FileOperationFailureException(
            FileOperationFailure.DELETE,
            null,
            "Failed to delete"
        )
        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.update(post, postUpdate)

        assertThat(result.title).isEqualTo(postUpdate.title)
        assertThat(result.content).isEqualTo(postUpdate.content)

        verify {
            fileService.deleteFile(testAttachment.id)
            postRepository.save(any())
        }
    }

    @Test
    fun `should delete post and its images`() {
        every { fileService.deleteFile(testAttachment.id) } just Runs
        every { postRepository.deleteById(post.id!!) } just Runs

        postService.delete(post)

        verify {
            fileService.deleteFile(testAttachment.id)
            postRepository.deleteById(post.id!!)
        }
    }

    @Test
    fun `should add comment to post and send notification to post author`() {
        // Create a different user as the commenter
        val commenter = User(
            id = "commenter-id",
            username = "commenter",
            email = "commenter@example.com",
            schoolInfo = user.schoolInfo.copy()
        )
        val commentContent = "Test Comment"

        every { postRepository.save(any()) } answers { firstArg() }
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())

        val result = postService.addCommentToPost(post, commentContent, commenter)

        assertThat(result.content).isEqualTo(commentContent)
        assertThat(result.author).isEqualTo(commenter)
        assertThat(post.comments).contains(result)

        // Verify notification was sent to post author
        verify(exactly = 1) { pushNotificationService.sendPushNotification(match { notification ->
            notification.recipient == post.author &&
            notification.sender == commenter &&
            notification.body == "commented on your post: ${commentContent}"
        })}

        verify { postRepository.save(post) }
    }

    @Test
    fun `should not send notification when commenting on own post`() {
        val commentContent = "Test Comment"

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.addCommentToPost(post, commentContent, user)

        assertThat(result.content).isEqualTo(commentContent)
        assertThat(result.author).isEqualTo(user)
        assertThat(post.comments).contains(result)

        verify(exactly = 0) { pushNotificationService.sendPushNotification(any()) }
        verify { postRepository.save(post) }
    }

    @Test
    fun `should add like to post and send notification`() {
        val liker = User(
            id = "liker-id",
            username = "liker",
            email = "liker@example.com",
            schoolInfo = user.schoolInfo.copy()
        )

        every { postRepository.addLikeToPost(post, liker.id!!) } returns true
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())

        val result = postService.addLikeToPost(post, liker)

        assertThat(result).isTrue()

        // Verify notification was sent to post author
        verify(exactly = 1) { pushNotificationService.sendPushNotification(match { notification ->
            notification.recipient == post.author &&
            notification.sender == liker &&
            notification.body == "Liked your post."
        })}
    }

    @Test
    fun `should not send notification when liking own post`() {
        every { postRepository.addLikeToPost(post, user.id!!) } returns true

        val result = postService.addLikeToPost(post, user)

        assertThat(result).isTrue()
        verify(exactly = 0) { pushNotificationService.sendPushNotification(any()) }
    }

    @Test
    fun `should add like to comment and send notification`() {
        val commentAuthor = User(
            id = "comment-author-id",
            username = "commentauthor",
            email = "commentauthor@example.com",
            schoolInfo = user.schoolInfo.copy()
        )
        val liker = User(
            id = "liker-id",
            username = "liker",
            email = "liker@example.com",
            schoolInfo = user.schoolInfo.copy()
        )
        val comment = Comment(
            id = ObjectId().toString(),
            content = "Test Comment",
            author = commentAuthor
        )
        post.comments.add(comment)

        every { postRepository.findById(post.id!!) } returns java.util.Optional.of(post)
        every { postRepository.addLikeToComment(post, comment.id.toString(), liker.id!!) } returns true
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())

        val result = postService.addLikeToComment(post, comment.id.toString(), liker)

        assertThat(result).isTrue()

        // Verify notification was sent to comment author
        verify(exactly = 1) { pushNotificationService.sendPushNotification(match { notification ->
            notification.recipient == commentAuthor &&
            notification.sender == liker &&
            notification.body == "Liked your comment."
        })}
    }

    @Test
    fun `should not send notification when liking own comment`() {
        val comment = Comment(
            id = ObjectId().toString(),
            content = "Test Comment",
            author = user
        )
        post.comments.add(comment)

        every { postRepository.findById(post.id!!) } returns java.util.Optional.of(post)
        every { postRepository.addLikeToComment(post, comment.id.toString(), user.id!!) } returns true

        val result = postService.addLikeToComment(post, comment.id.toString(), user)

        assertThat(result).isTrue()
        verify(exactly = 0) { pushNotificationService.sendPushNotification(any()) }
    }

    @Test
    fun `should add like to reply and send notification`() {
        val replyAuthor = User(
            id = "reply-author-id",
            username = "replyauthor",
            email = "replyauthor@example.com",
            schoolInfo = user.schoolInfo.copy()
        )
        val liker = User(
            id = "liker-id",
            username = "liker",
            email = "liker@example.com",
            schoolInfo = user.schoolInfo.copy()
        )
        val comment = Comment(
            id = ObjectId().toString(),
            content = "Test Comment",
            author = user
        )
        val reply = Reply(
            id = ObjectId().toString(),
            content = "Test Reply",
            author = replyAuthor
        )
        comment.replies.add(reply)
        post.comments.add(comment)

        every { postRepository.findById(post.id!!) } returns java.util.Optional.of(post)
        every { postRepository.addLikeToReply(post, comment.id.toString(), reply.id.toString(), liker.id!!) } returns true
        every { pushNotificationService.sendPushNotification(any()) } returns CompletableFuture.completedFuture(mockk())

        val result = postService.addLikeToReply(post, comment.id.toString(), reply.id.toString(), liker)

        assertThat(result).isTrue()

        // Verify notification was sent to reply author
        verify(exactly = 1) { pushNotificationService.sendPushNotification(match { notification ->
            notification.recipient == replyAuthor &&
            notification.sender == liker &&
            notification.body == "Liked your reply."
        })}
    }

    @Test
    fun `should not send notification when liking own reply`() {
        val comment = Comment(
            id = ObjectId().toString(),
            content = "Test Comment",
            author = user
        )
        val reply = Reply(
            id = ObjectId().toString(),
            content = "Test Reply",
            author = user
        )
        comment.replies.add(reply)
        post.comments.add(comment)

        every { postRepository.findById(post.id!!) } returns java.util.Optional.of(post)
        every { postRepository.addLikeToReply(post, comment.id.toString(), reply.id.toString(), user.id!!) } returns true

        val result = postService.addLikeToReply(post, comment.id.toString(), reply.id.toString(), user)

        assertThat(result).isTrue()
        verify(exactly = 0) { pushNotificationService.sendPushNotification(any()) }
    }
}
