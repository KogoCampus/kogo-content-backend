package com.kogo.content.storage.view

import com.kogo.content.endpoint.`test-util`.Fixture
import com.kogo.content.exception.InvalidFieldException
import com.kogo.content.lib.FilterOperator
import com.kogo.content.lib.PageToken
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.SortDirection
import com.kogo.content.storage.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertFails

@SpringBootTest
@ActiveProfiles("test")
class CommentAggregateViewTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate,
    private val commentAggregateView: CommentAggregateView
) {
    private lateinit var user: User
    private lateinit var topic: Topic
    private lateinit var post1: Post
    private lateinit var post2: Post
    private lateinit var post1Comments: List<Comment>
    private lateinit var post2Comments: List<Comment>

    @BeforeEach
    fun setup() {
        // Clear collections
        mongoTemplate.dropCollection(Comment::class.java)
        mongoTemplate.dropCollection(Like::class.java)
        mongoTemplate.dropCollection(Reply::class.java)
        mongoTemplate.dropCollection("comment_stats")
        mongoTemplate.dropCollection(Post::class.java)
        mongoTemplate.dropCollection(User::class.java)
        mongoTemplate.dropCollection(Topic::class.java)

        // Create base test data
        user = Fixture.createUserFixture()
        mongoTemplate.save(user)

        topic = Fixture.createTopicFixture(owner = user)
        mongoTemplate.save(topic)

        // Create two posts
        post1 = Fixture.createPostFixture(topic = topic, author = user)
        post2 = Fixture.createPostFixture(topic = topic, author = user)
        mongoTemplate.save(post1)
        mongoTemplate.save(post2)

        // Create comments for post1 with timestamps in reverse order
        // so that higher numbered comments are newer
        val now = Instant.now()
        post1Comments = (1..5).map { i ->
            val comment = Fixture.createCommentFixture(post = post1, author = user).copy(
                content = "Post1 Comment $i",
                // Higher number = newer timestamp
                createdAt = now.plusSeconds(i * 100L)
            )
            mongoTemplate.save(comment)

            // Add likes
            repeat(i) { j ->
                mongoTemplate.save(Like(likableId = ObjectId(comment.id), userId = "user-$j"))
            }

            // Add replies
            repeat(i % 2) {
                val reply = Fixture.createReplyFixture(comment = comment, author = user)
                mongoTemplate.save(reply)
            }

            comment
        }

        // Create comments for post2
        post2Comments = (1..3).map { i ->
            val comment = Fixture.createCommentFixture(post = post2, author = user).copy(
                content = "Post2 Comment $i",
                createdAt = now.plusSeconds(i * 100L)
            )
            mongoTemplate.save(comment)

            // Add different number of likes
            repeat(i + 2) { j ->
                mongoTemplate.save(Like(likableId = ObjectId(comment.id), userId = "user-$j"))
            }

            // Add replies
            repeat((i + 1) % 2) {
                val reply = Fixture.createReplyFixture(comment = comment, author = user)
                mongoTemplate.save(reply)
            }

            comment
        }

        // Refresh views for all comments
        (post1Comments + post2Comments).forEach {
            commentAggregateView.refreshView(it.id!!)
        }

        // Add debug logging to verify the view data
        post1Comments.forEach { comment ->
            commentAggregateView.refreshView(comment.id!!)
            mongoTemplate.findById(comment.id!!, CommentAggregate::class.java, "comment_stats")
        }
    }

    private fun printCollectionState() {
        println("\nComment Stats Collection state:")
        mongoTemplate.findAll(CommentAggregate::class.java, "comment_stats").forEach { aggregate ->
            println("ID: ${aggregate.commentId}")
            println("Post ID: ${aggregate.comment.post}")
            println("Content: ${aggregate.comment.content}")
            println("Created At: ${aggregate.comment.createdAt}")
            println("---")
        }
    }

    @Test
    fun `should encode and decode page token correctly`() {
        val initialRequest = PaginationRequest(limit = 2)
            .withFilter("post", post1.id!!)
            .withSort("createdAt", SortDirection.DESC)

        // First page
        val firstPage = commentAggregateView.findAll(initialRequest)
        assertThat(firstPage.items).hasSize(2)
        assertThat(firstPage.nextPageToken).isNotNull
        assertThat(firstPage.items.map { it.comment.post.id }).containsOnly(post1.id)
        assertThat(firstPage.items.map { it.comment.content })
            .containsExactly("Post1 Comment 5", "Post1 Comment 4")

        // Encode and decode token
        val encodedToken = firstPage.nextPageToken?.encode()
        assertThat(encodedToken).isNotNull

        val decodedToken = PageToken.fromString(encodedToken!!)
        assertThat(decodedToken.filters).anySatisfy { filter ->
            assertThat(filter.field).isEqualTo("post")
            assertThat(filter.value).isEqualTo(post1.id)
        }
        assertThat(decodedToken.sortFields).anySatisfy{ sortField ->
            assertThat(sortField.field).isEqualTo("createdAt")
            assertThat(sortField.direction).isEqualTo(SortDirection.DESC)
        }

        // Next page using decoded token
        val nextPage = commentAggregateView.findAll(
            PaginationRequest(
                limit = 2,
                pageToken = decodedToken
            )
        )

        assertThat(nextPage.items).hasSize(2)
        assertThat(nextPage.items.map { it.comment.post.id }).containsOnly(post1.id)
        assertThat(nextPage.items.map { it.comment.content })
            .containsExactly("Post1 Comment 3", "Post1 Comment 2")
    }

    @Test
    fun `should get all comments by post with pagination and sorting`() {
        val request = PaginationRequest(limit = 2)
            .withFilter("post", post1.id!!)
            .withSort("createdAt", SortDirection.DESC)

        // First page - should get newest comments first
        val firstPage = commentAggregateView.findAll(request)
        assertThat(firstPage.items).hasSize(2)
        assertThat(firstPage.nextPageToken).isNotNull
        assertThat(firstPage.items.map { it.comment.content })
            .containsExactly("Post1 Comment 5", "Post1 Comment 4")

        // Second page
        val secondPage = commentAggregateView.findAll(
            PaginationRequest(
                limit = 2,
                pageToken = firstPage.nextPageToken!!
            )
        )
        assertThat(secondPage.items).hasSize(2)
        assertThat(secondPage.items.map { it.comment.content })
            .containsExactly("Post1 Comment 3", "Post1 Comment 2")

        // Last page
        val lastPage = commentAggregateView.findAll(
            PaginationRequest(
                limit = 2,
                pageToken = secondPage.nextPageToken!!
            )
        )
        assertThat(lastPage.items).hasSize(1)
        assertThat(lastPage.nextPageToken).isNull()
        assertThat(lastPage.items.map { it.comment.content })
            .containsExactly("Post1 Comment 1")
    }

    @Test
    fun `should handle multiple filters and sorts`() {
        val request = PaginationRequest(limit = 10)
            .withFilter("post", post1.id!!)
            .withFilter("likeCount", 3, FilterOperator.IN)
            .withSort("likeCount", SortDirection.DESC)
            .withSort("createdAt", SortDirection.DESC)

        val result = commentAggregateView.findAll(request)

        assertThat(result.items).isNotEmpty
        assertThat(result.items.map { it.likeCount })
            .isSortedAccordingTo(Comparator.reverseOrder())

        // Verify secondary sort when like counts are equal
        val samelikeCountComments = result.items.groupBy { it.likeCount }
            .filter { it.value.size > 1 }
            .values
            .flatten()

        assertThat(samelikeCountComments.map { it.comment.createdAt })
            .isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    fun `should handle filter with IN operator`() {
        val request = PaginationRequest(limit = 10)
            .withFilter("post", post1.id!!)
            .withFilter("likeCount", listOf(1, 3, 5), FilterOperator.IN)
            .withSort("likeCount", SortDirection.DESC)

        val result = commentAggregateView.findAll(request)

        assertThat(result.items).isNotEmpty
        assertThat(result.items.map { it.likeCount }).allMatch { it in listOf(1, 3, 5) }
    }

    @Test
    fun `should aggregate comment stats correctly`() {
        val comment = post1Comments[4] // Last comment has 5 likes and 1 reply
        val stats = commentAggregateView.find(comment.id!!)!!

        assertThat(stats.comment.id).isEqualTo(comment.id)
        assertThat(stats.comment.content).isEqualTo("Post1 Comment 5")
        assertThat(stats.likeCount).isEqualTo(5)
        assertThat(stats.replyCount).isEqualTo(1)
        assertThat(stats.likedUserIds).containsExactly("user-0", "user-1", "user-2", "user-3", "user-4")
    }

    @Test
    fun `should throw exception for invalid field`() {
        assertThrows<InvalidFieldException> {
            commentAggregateView.findAll(
                PaginationRequest(limit = 3)
                    .withFilter("invalidField", "value")
            )
        }
    }

    @Test
    fun `should handle empty results`() {
        val result = commentAggregateView.findAll(
            PaginationRequest(limit = 10)
                .withFilter("post", "non-existent-post-id")
        )

        assertThat(result.items).isEmpty()
        assertThat(result.nextPageToken).isNull()
    }
}
