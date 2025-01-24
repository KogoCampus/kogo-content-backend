package com.kogo.content.storage.repository

import com.kogo.content.storage.model.Attachment
import com.kogo.content.storage.model.Comment
import com.kogo.content.storage.model.Like
import com.kogo.content.storage.model.Reply
import com.kogo.content.storage.model.entity.Group
import com.kogo.content.storage.model.entity.Post
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class PostRepositoryTest @Autowired constructor(
    private val postRepository: PostRepository,
    private val mongoTemplate: MongoTemplate
) {
    private lateinit var testUser: User
    private lateinit var testGroup: Group
    private lateinit var testPost: Post
    private lateinit var testComment: Comment
    private lateinit var testReply: Reply

    @BeforeEach
    fun setup() {
        // Clear collections
        mongoTemplate.dropCollection(Post::class.java)
        mongoTemplate.dropCollection(User::class.java)
        mongoTemplate.dropCollection(Group::class.java)

        // Create test user
        testUser = User(
            id = "test-user-id",
            username = "testuser",
            email = "test@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            )
        )
        mongoTemplate.save(testUser)

        // Create test group
        testGroup = Group(
            id = "test-group-id",
            groupName = "Test Group",
            description = "Test Description",
            owner = testUser,
            tags = mutableListOf("test", "group")
        )
        mongoTemplate.save(testGroup)

        // Create test comment and reply
        testReply = Reply(
            id = "test-reply-id",
            content = "Test Reply",
            author = testUser
        )

        testComment = Comment(
            id = "test-comment-id",
            content = "Test Comment",
            author = testUser,
            replies = mutableListOf(testReply)
        )

        // Create test post
        testPost = Post(
            id = "test-post-id",
            title = "Test Post",
            content = "Test Content",
            group = testGroup,
            author = testUser,
            comments = mutableListOf(testComment)
        )
        mongoTemplate.save(testPost)
    }

    @Test
    fun `should add like to post`() {
        val result = postRepository.addLikeToPost(testPost, testUser.id!!)

        assertThat(result).isTrue()
        val updatedPost = mongoTemplate.findById(testPost.id!!, Post::class.java)!!
        assertThat(updatedPost.likes).hasSize(1)
        assertThat(updatedPost.likes.first().userId).isEqualTo(testUser.id)
        assertThat(updatedPost.likes.first().isActive).isTrue()
    }

    @Test
    fun `should not add duplicate active like to post`() {
        // Add initial like
        postRepository.addLikeToPost(testPost, testUser.id!!)

        // Try to add duplicate like
        val result = postRepository.addLikeToPost(testPost, testUser.id!!)

        assertThat(result).isFalse()
        val updatedPost = mongoTemplate.findById(testPost.id!!, Post::class.java)!!
        assertThat(updatedPost.likes).hasSize(1)
    }

    @Test
    fun `should reactivate inactive like on post`() {
        // Add initial like and deactivate it
        postRepository.addLikeToPost(testPost, testUser.id!!)
        postRepository.removeLikeFromPost(testPost, testUser.id!!)

        // Reactivate like
        val result = postRepository.addLikeToPost(testPost, testUser.id!!)

        assertThat(result).isTrue()
        val updatedPost = mongoTemplate.findById(testPost.id!!, Post::class.java)!!
        assertThat(updatedPost.likes).hasSize(1)
        assertThat(updatedPost.likes.first().isActive).isTrue()
    }

    @Test
    fun `should add like to comment`() {
        val result = postRepository.addLikeToComment(testPost, testComment.id, testUser.id!!)

        assertThat(result).isTrue()
        val updatedPost = mongoTemplate.findById(testPost.id!!, Post::class.java)!!
        val updatedComment = updatedPost.comments.first()
        assertThat(updatedComment.likes).hasSize(1)
        assertThat(updatedComment.likes.first().userId).isEqualTo(testUser.id)
        assertThat(updatedComment.likes.first().isActive).isTrue()
    }

    @Test
    fun `should add like to reply`() {
        val result = postRepository.addLikeToReply(testPost, testComment.id, testReply.id, testUser.id!!)

        assertThat(result).isTrue()
        val updatedPost = mongoTemplate.findById(testPost.id!!, Post::class.java)!!
        val updatedReply = updatedPost.comments.first().replies.first()
        assertThat(updatedReply.likes).hasSize(1)
        assertThat(updatedReply.likes.first().userId).isEqualTo(testUser.id)
        assertThat(updatedReply.likes.first().isActive).isTrue()
    }

    @Test
    fun `should remove like from post`() {
        // Add like first
        postRepository.addLikeToPost(testPost, testUser.id!!)

        // Remove like
        val result = postRepository.removeLikeFromPost(testPost, testUser.id!!)

        assertThat(result).isTrue()
        val updatedPost = mongoTemplate.findById(testPost.id!!, Post::class.java)!!
        assertThat(updatedPost.likes).hasSize(1)
        assertThat(updatedPost.likes.first().isActive).isFalse()
    }

    @Test
    fun `should handle non-existent comment when adding like`() {
        val result = postRepository.addLikeToComment(testPost, "non-existent-id", testUser.id!!)
        assertThat(result).isFalse()
    }

    @Test
    fun `should handle non-existent reply when adding like`() {
        val result = postRepository.addLikeToReply(testPost, testComment.id, "non-existent-id", testUser.id!!)
        assertThat(result).isFalse()
    }
} 