package com.kogo.content.storage.repository.trait

import com.kogo.content.storage.entity.Like
import com.kogo.content.storage.entity.View
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@SpringBootTest
class LikableTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    private val likable: Likable = LikableImpl(mongoTemplate)

    @BeforeEach
    fun setup() {
        // Clean up collections before each test
        mongoTemplate.dropCollection(Like::class.java)
        mongoTemplate.dropCollection(View::class.java)
    }

    @AfterEach
    fun cleanup() {
        // Clean up collections after each test
        mongoTemplate.dropCollection(Like::class.java)
        mongoTemplate.dropCollection(View::class.java)
    }

    @Test
    fun `should add like when user hasn't liked before`() {
        val userId = "test-user-id"
        val likableId = "test-likable-id"

        val result = likable.addLike(likableId, userId)

        assertThat(result).isNotNull
        assertThat(result?.userId).isEqualTo(userId)
        assertThat(result?.likableId).isEqualTo(likableId)

        // Verify like exists in database
        val savedLike = mongoTemplate.findOne(
            Query(Criteria.where("userId").`is`(userId).and("likableId").`is`(likableId)),
            Like::class.java
        )
        assertThat(savedLike).isNotNull
        assertThat(savedLike?.userId).isEqualTo(userId)
        assertThat(savedLike?.likableId).isEqualTo(likableId)
    }

    @Test
    fun `should not add like when user has already liked`() {
        val userId = "test-user-id"
        val likableId = "test-likable-id"

        // Add initial like
        likable.addLike(likableId, userId)

        // Try to add like again
        val result = likable.addLike(likableId, userId)

        assertThat(result).isNull()

        // Verify only one like exists in database
        val likes = mongoTemplate.find(
            Query(Criteria.where("userId").`is`(userId).and("likableId").`is`(likableId)),
            Like::class.java
        )
        assertThat(likes).hasSize(1)
    }

    @Test
    fun `should remove like when it exists`() {
        val userId = "test-user-id"
        val likableId = "test-likable-id"

        // Add initial like
        likable.addLike(likableId, userId)

        val result = likable.removeLike(likableId, userId)

        assertThat(result).isTrue()

        // Verify like was removed from database
        val like = mongoTemplate.findOne(
            Query(Criteria.where("userId").`is`(userId).and("likableId").`is`(likableId)),
            Like::class.java
        )
        assertThat(like).isNull()
    }

    @Test
    fun `should find existing like`() {
        val userId = "test-user-id"
        val likableId = "test-likable-id"

        // Add like to find
        likable.addLike(likableId, userId)

        val result = likable.findLike(likableId, userId)

        assertThat(result).isNotNull
        assertThat(result?.userId).isEqualTo(userId)
        assertThat(result?.likableId).isEqualTo(likableId)
    }

    @Test
    fun `should add view when user hasn't viewed before`() {
        val userId = "test-user-id"
        val viewableId = "test-viewable-id"

        val result = likable.addViewCount(viewableId, userId)

        assertThat(result).isNotNull
        assertThat(result?.userId).isEqualTo(userId)
        assertThat(result?.viewableId).isEqualTo(viewableId)

        // Verify view exists in database
        val savedView = mongoTemplate.findOne(
            Query(Criteria.where("userId").`is`(userId).and("viewableId").`is`(viewableId)),
            View::class.java
        )
        assertThat(savedView).isNotNull
        assertThat(savedView?.userId).isEqualTo(userId)
        assertThat(savedView?.viewableId).isEqualTo(viewableId)
    }

    @Test
    fun `should not add view when user has already viewed`() {
        val userId = "test-user-id"
        val viewableId = "test-viewable-id"

        // Add initial view
        likable.addViewCount(viewableId, userId)

        // Try to add view again
        val result = likable.addViewCount(viewableId, userId)

        assertThat(result).isNull()

        // Verify only one view exists in database
        val views = mongoTemplate.find(
            Query(Criteria.where("userId").`is`(userId).and("viewableId").`is`(viewableId)),
            View::class.java
        )
        assertThat(views).hasSize(1)
    }
}
