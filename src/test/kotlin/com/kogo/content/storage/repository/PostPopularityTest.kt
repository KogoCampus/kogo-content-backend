package com.kogo.content.storage.repository

import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.UserDetails
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
class PostPopularityTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    private val postPopularity: PostPopularity = PostPopularityImpl(mongoTemplate)
    private lateinit var testUser: UserDetails
    private lateinit var testTopic: Topic

    @BeforeEach
    fun setup() {
        mongoTemplate.dropCollection(Post::class.java)
        mongoTemplate.dropCollection(Topic::class.java)
        mongoTemplate.dropCollection(UserDetails::class.java)

        // Create and save test user first
        testUser = UserDetails(
            id = "test-user-id",
            username = "test-user"
        )
        mongoTemplate.save(testUser)

        // Create and save test topic with the saved user
        testTopic = Topic(
            id = "test-topic-id",
            topicName = "test-topic",
            owner = testUser
        )
        mongoTemplate.save(testTopic)
    }

    @AfterEach
    fun cleanup() {
        mongoTemplate.dropCollection(Post::class.java)
        mongoTemplate.dropCollection(Topic::class.java)
        mongoTemplate.dropCollection(UserDetails::class.java)
    }

    private fun createPost(
        id: String,
        likes: Int = 0,
        commentCount: Int = 0,
        viewCount: Int = 0,
        createdAt: Instant = Instant.now()
    ): Post {
        return Post(
            id = id,
            title = "Test Post $id",
            content = "Test Content",
            author = testUser,
            topic = testTopic,
            likes = likes,
            commentCount = commentCount,
            viewCount = viewCount,
            createdAt = createdAt
        )
    }

    @Test
    fun `should return posts ordered by popularity score`() {
        val now = Instant.now()
        // Create posts with different popularity metrics
        val highPopularityPost = createPost(
            id = "high-popularity",
            likes = 100,
            commentCount = 50,
            viewCount = 1000,
            createdAt = now
        )
        val mediumPopularityPost = createPost(
            id = "medium-popularity",
            likes = 50,
            commentCount = 25,
            viewCount = 500,
            createdAt = now
        )
        val lowPopularityPost = createPost(
            id = "low-popularity",
            likes = 10,
            commentCount = 5,
            viewCount = 100,
            createdAt = now
        )

        // Save posts in random order
        mongoTemplate.save(mediumPopularityPost)
        mongoTemplate.save(lowPopularityPost)
        mongoTemplate.save(highPopularityPost)

        val results = postPopularity.findAllPopular(0, 10)
        println(results[0].toString())
        println(results[1].toString())
        println(results[2].toString())
        assertThat(results).hasSize(3)
        assertThat(results[0].id).isEqualTo(highPopularityPost.id)
        assertThat(results[1].id).isEqualTo(mediumPopularityPost.id)
        assertThat(results[2].id).isEqualTo(lowPopularityPost.id)
    }

    @Test
    fun `should exclude posts older than 14 days`() {
        val now = Instant.now()
        val recentPost = createPost(
            id = "recent-post",
            createdAt = now
        )
        val oldPost = createPost(
            id = "old-post",
            createdAt = now.minus(15, ChronoUnit.DAYS)
        )

        mongoTemplate.save(recentPost)
        mongoTemplate.save(oldPost)

        val results = postPopularity.findAllPopular(0, 10)

        assertThat(results).hasSize(1)
        assertThat(results[0].id).isEqualTo(recentPost.id)
    }

    @Test
    fun `should respect pagination parameters`() {
        val now = Instant.now()
        // Create and save 5 posts with increasing popularity
        val posts = (1..5).map { i ->
            createPost(
                id = "post-$i",
                likes = i * 20,
                commentCount = i * 10,
                viewCount = i * 100,
                createdAt = now
            )
        }
        posts.forEach { mongoTemplate.save(it) }

        // Test first page (2 items)
        val firstPage = postPopularity.findAllPopular(0, 2)
        assertThat(firstPage).hasSize(2)
        assertThat(firstPage[0].id).isEqualTo("post-5") // Highest popularity
        assertThat(firstPage[1].id).isEqualTo("post-4")

        // Test second page (2 items)
        val secondPage = postPopularity.findAllPopular(1, 2)
        assertThat(secondPage).hasSize(2)
        assertThat(secondPage[0].id).isEqualTo("post-3")
        assertThat(secondPage[1].id).isEqualTo("post-2")

        // Test last page (1 item)
        val lastPage = postPopularity.findAllPopular(2, 2)
        assertThat(lastPage).hasSize(1)
        assertThat(lastPage[0].id).isEqualTo("post-1") // Lowest popularity
    }

    @Test
    fun `should handle empty collection`() {
        val results = postPopularity.findAllPopular(0, 10)
        assertThat(results).isEmpty()
    }
}
