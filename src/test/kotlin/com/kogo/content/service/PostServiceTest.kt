package com.kogo.content.service

import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.endpoint.model.PostUpdate
import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import com.kogo.content.endpoint.common.SortDirection
import com.kogo.content.search.SearchIndex
import com.kogo.content.storage.model.Comment
import com.kogo.content.storage.model.Like
import com.kogo.content.storage.model.Reply
import com.kogo.content.storage.model.entity.*
import com.kogo.content.storage.pagination.MongoPaginationQueryBuilder
import com.kogo.content.storage.repository.PostRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class PostServiceTest {
    private val postRepository: PostRepository = mockk()
    private val postSearchIndex: SearchIndex<Post> = mockk()

    private val mongoPaginationQueryBuilder: MongoPaginationQueryBuilder = mockk()

    private val postService = PostService(
        postRepository = postRepository,
        postSearchIndex = postSearchIndex
    ).apply {
        mongoPaginationQueryBuilder = this@PostServiceTest.mongoPaginationQueryBuilder
    }

    private lateinit var user: User
    private lateinit var group: Group
    private lateinit var post: Post

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
            followers = mutableListOf(Follower(user)),
        )

        post = Post(
            id = "test-post-id",
            title = "Test Post",
            content = "Test Content",
            group = group,
            author = user,
            attachments = mutableListOf(),
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
            images = emptyList(),
            videos = emptyList()
        )

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.create(group, user, postDto)

        assertThat(result.title).isEqualTo(postDto.title)
        assertThat(result.content).isEqualTo(postDto.content)
        assertThat(result.group).isEqualTo(group)
        assertThat(result.author).isEqualTo(user)
        verify { postRepository.save(any()) }
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
        every { postRepository.deleteById(post.id!!) } just Runs

        postService.delete(post)

        verify { postRepository.deleteById(post.id!!) }
    }

    @Test
    fun `should add comment to post`() {
        val commentContent = "Test Comment"

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.addCommentToPost(post, commentContent, user)

        assertThat(result.content).isEqualTo(commentContent)
        assertThat(result.author).isEqualTo(user)
        assertThat(post.comments).contains(result)
        verify { postRepository.save(post) }
    }

    @Test
    fun `should remove comment from post`() {
        val comment = Comment(
            id = ObjectId(),
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
            id = ObjectId(),
            content = "Test Comment",
            author = user
        )
        post.comments.add(comment)
        val replyContent = "Test Reply"

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.addReplyToComment(post, comment.id.toString(), replyContent, user)

        assertThat(result.content).isEqualTo(replyContent)
        assertThat(result.author).isEqualTo(user)
        assertThat(comment.replies).contains(result)
        verify { postRepository.save(post) }
    }

    @Test
    fun `should add like to post`() {
        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.addLikeToPost(post, user)

        assertThat(result).isTrue()
        assertThat(post.likes).hasSize(1)
        assertThat(post.likes.first().userId).isEqualTo(user.id)
        assertThat(post.likes.first().isActive).isTrue()
        verify { postRepository.save(post) }
    }

    @Test
    fun `should not add duplicate active like to post`() {
        post.likes.add(Like(userId = user.id!!, isActive = true, updatedAt = System.currentTimeMillis()))

        val result = postService.addLikeToPost(post, user)

        assertThat(result).isFalse()
        assertThat(post.likes).hasSize(1)
        verify(exactly = 0) { postRepository.save(any()) }
    }

    @Test
    fun `should reactivate inactive like on post`() {
        post.likes.add(Like(userId = user.id!!, isActive = false, updatedAt = System.currentTimeMillis()))

        every { postRepository.save(any()) } answers { firstArg() }

        val result = postService.addLikeToPost(post, user)

        assertThat(result).isTrue()
        assertThat(post.likes).hasSize(1)
        assertThat(post.likes.first().isActive).isTrue()
        verify { postRepository.save(post) }
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
    fun `should search posts`() {
        val searchKeyword = "test"
        val paginationRequest = PaginationRequest(limit = 10)
        val expectedResult = PaginationSlice(items = listOf(post))

        every {
            postSearchIndex.search(
                searchText = searchKeyword,
                paginationRequest = paginationRequest
            )
        } returns expectedResult

        val result = postService.search(searchKeyword, paginationRequest)

        assertThat(result).isEqualTo(expectedResult)
        verify {
            postSearchIndex.search(
                searchText = searchKeyword,
                paginationRequest = paginationRequest
            )
        }
    }
}
