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
class ReplyAggregateViewTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate,
    private val replyAggregateView: ReplyAggregateView
) {
    private lateinit var user: User
    private lateinit var topic: Topic
    private lateinit var post: Post
    private lateinit var comment: Comment
    private lateinit var reply: Reply

    @BeforeEach
    fun setup() {
        // Clear collections
        mongoTemplate.dropCollection(Reply::class.java)
        mongoTemplate.dropCollection(Like::class.java)
        mongoTemplate.dropCollection("reply_stats")
        mongoTemplate.dropCollection(Comment::class.java)
        mongoTemplate.dropCollection(Post::class.java)
        mongoTemplate.dropCollection(User::class.java)
        mongoTemplate.dropCollection(Topic::class.java)

        // Create test data using fixtures
        user = Fixture.createUserFixture()
        mongoTemplate.save(user)

        topic = Fixture.createTopicFixture(owner = user)
        mongoTemplate.save(topic)

        post = Fixture.createPostFixture(topic = topic, author = user)
        mongoTemplate.save(post)

        comment = Fixture.createCommentFixture(post = post, author = user)
        mongoTemplate.save(comment)

        reply = Fixture.createReplyFixture(comment = comment, author = user)
        mongoTemplate.save(reply)
    }

    @Test
    fun `should aggregate reply stats with reply document and interactions`() {
        // Create likes
        val likes = (1..3).map { i ->
            Like(id = "like-$i", likableId = ObjectId(reply.id), userId = "user-$i")
        }
        mongoTemplate.insertAll(likes)

        // Get stats
        val replyStat = replyAggregateView.find(reply.id!!)

        // Verify reply document
        assertThat(replyStat.reply).isNotNull
        assertThat(replyStat.reply.id).isEqualTo(reply.id)
        assertThat(replyStat.reply.content).isEqualTo(reply.content)
        assertThat(replyStat.reply.author.id).isEqualTo(user.id)
        assertThat(replyStat.reply.comment.id).isEqualTo(comment.id)

        // Verify interactions
        assertThat(replyStat.replyId).isEqualTo(reply.id)
        assertThat(replyStat.likedUserIds).containsExactlyInAnyOrder("user-1", "user-2", "user-3")
        assertThat(replyStat.likeCount).isEqualTo(3)
    }

    @Test
    fun `should handle multiple replies independently`() {
        // Create another reply
        val reply2 = Fixture.createReplyFixture(comment = comment, author = user)
            .copy(id = ObjectId().toString())
        mongoTemplate.save(reply2)

        // Add different likes
        val likes = listOf(
            Like(id = "like-1", likableId = ObjectId(reply.id), userId = "user-1"),
            Like(id = "like-2", likableId = ObjectId(reply2.id), userId = "user-2")
        )
        mongoTemplate.insertAll(likes)

        // Verify stats
        val stat1 = replyAggregateView.find(reply.id!!)
        val stat2 = replyAggregateView.find(reply2.id!!)

        assertThat(stat1.likeCount).isEqualTo(1)
        assertThat(stat1.likedUserIds).containsExactly("user-1")
        assertThat(stat2.likeCount).isEqualTo(1)
        assertThat(stat2.likedUserIds).containsExactly("user-2")
    }

    @Test
    fun `should handle document reference integrity`() {
        val replyStat = replyAggregateView.find(reply.id!!)

        // Verify all references
        assertThat(replyStat.reply.author.id).isEqualTo(user.id)
        assertThat(replyStat.reply.comment.id).isEqualTo(comment.id)
        assertThat(replyStat.reply.comment.author.id).isEqualTo(user.id)
        assertThat(replyStat.reply.comment.post.id).isEqualTo(post.id)
    }
}
