package com.kogo.content.storage.view

import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.storage.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class TopicAggregateViewTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate,
    private val topicAggregateView: TopicAggregateView
) {
    private lateinit var user: User
    private lateinit var topic1: Topic
    private lateinit var topic2: Topic
    private lateinit var topic1Posts: List<Post>
    private lateinit var topic2Posts: List<Post>

    @BeforeEach
    fun setup() {
        // Clear collections
        mongoTemplate.dropCollection(Post::class.java)
        mongoTemplate.dropCollection(User::class.java)
        mongoTemplate.dropCollection(Topic::class.java)
        mongoTemplate.dropCollection(Follower::class.java)
        mongoTemplate.dropCollection("topic_stats")

        // Create base test data
        user = Fixture.createUserFixture()
        mongoTemplate.save(user)

        // Create two topics
        topic1 = Fixture.createTopicFixture(owner = user)
        topic2 = Fixture.createTopicFixture(owner = user)
        mongoTemplate.save(topic1)
        mongoTemplate.save(topic2)

        // Create posts for topic1
        topic1Posts = (1..5).map { i ->
            val post = Fixture.createPostFixture(topic = topic1, author = user)
            mongoTemplate.save(post)
            post
        }

        // Create posts for topic2
        topic2Posts = (1..3).map { i ->
            val post = Fixture.createPostFixture(topic = topic2, author = user)
            mongoTemplate.save(post)
            post
        }

        // Add followers to topics
        (1..4).forEach { i ->
            mongoTemplate.save(Follower(followableId = ObjectId(topic1.id), userId = "user-$i"))
        }
        (1..2).forEach { i ->
            mongoTemplate.save(Follower(followableId = ObjectId(topic2.id), userId = "user-$i"))
        }

        // Refresh views for all topics
        listOf(topic1, topic2).forEach {
            topicAggregateView.refreshView(it.id!!)
        }
    }

    @Test
    fun `should aggregate topic stats correctly`() {
        // Test topic1 stats
        val topic1Stats = topicAggregateView.find(topic1.id!!)
        assertThat(topic1Stats.topic.id).isEqualTo(topic1.id)
        assertThat(topic1Stats.postCount).isEqualTo(5)
        assertThat(topic1Stats.postIds).hasSize(5)
        assertThat(topic1Stats.postIds).containsExactlyInAnyOrderElementsOf(
            topic1Posts.map { it.id }
        )
        assertThat(topic1Stats.followerCount).isEqualTo(4)
        assertThat(topic1Stats.followerIds).hasSize(4)
        assertThat(topic1Stats.followerIds).containsExactlyInAnyOrder(
            "user-1", "user-2", "user-3", "user-4"
        )

        // Test topic2 stats
        val topic2Stats = topicAggregateView.find(topic2.id!!)
        assertThat(topic2Stats.topic.id).isEqualTo(topic2.id)
        assertThat(topic2Stats.postCount).isEqualTo(3)
        assertThat(topic2Stats.postIds).hasSize(3)
        assertThat(topic2Stats.postIds).containsExactlyInAnyOrderElementsOf(
            topic2Posts.map { it.id }
        )
        assertThat(topic2Stats.followerCount).isEqualTo(2)
        assertThat(topic2Stats.followerIds).hasSize(2)
        assertThat(topic2Stats.followerIds).containsExactlyInAnyOrder(
            "user-1", "user-2"
        )
    }

    @Test
    fun `should handle topic with no posts or followers`() {
        val emptyTopic = Fixture.createTopicFixture(owner = user)
        mongoTemplate.save(emptyTopic)
        topicAggregateView.refreshView(emptyTopic.id!!)

        val stats = topicAggregateView.find(emptyTopic.id!!)
        assertThat(stats.topic.id).isEqualTo(emptyTopic.id)
        assertThat(stats.postCount).isEqualTo(0)
        assertThat(stats.followerCount).isEqualTo(0)
        assertThat(stats.followerIds).isEmpty()
    }

    @Test
    fun `should update stats when posts or followers are added`() {
        // Add new post to topic1
        val newPost = Fixture.createPostFixture(topic = topic1, author = user)
        mongoTemplate.save(newPost)

        // Add new follower to topic1
        mongoTemplate.save(Follower(followableId = ObjectId(topic1.id), userId = "user-5"))

        // Refresh view
        topicAggregateView.refreshView(topic1.id!!)

        // Verify updated stats
        val updatedStats = topicAggregateView.find(topic1.id!!)
        assertThat(updatedStats.postCount).isEqualTo(6)
        assertThat(updatedStats.followerCount).isEqualTo(5)
        assertThat(updatedStats.followerIds).hasSize(5)
        assertThat(updatedStats.followerIds).contains("user-5")
    }
}
