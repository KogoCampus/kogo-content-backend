package com.kogo.content.storage.repository.trait

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
class ViewableTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    private val viewable: Viewable = ViewableImpl(mongoTemplate)

    @BeforeEach
    fun setup() {
        mongoTemplate.dropCollection(View::class.java)
    }

    @AfterEach
    fun cleanup() {
        mongoTemplate.dropCollection(View::class.java)
    }

    @Test
    fun `should add view when user hasn't viewed before`() {
        val userId = "test-user-id"
        val viewableId = "test-viewable-id"

        val result = viewable.addView(viewableId, userId)

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
        viewable.addView(viewableId, userId)

        // Try to add view again
        val result = viewable.addView(viewableId, userId)

        assertThat(result).isNull()

        // Verify only one view exists in database
        val views = mongoTemplate.find(
            Query(Criteria.where("userId").`is`(userId).and("viewableId").`is`(viewableId)),
            View::class.java
        )
        assertThat(views).hasSize(1)
    }

    @Test
    fun `should remove view when it exists`() {
        val userId = "test-user-id"
        val viewableId = "test-viewable-id"

        // Add initial view
        viewable.addView(viewableId, userId)

        val result = viewable.removeView(viewableId, userId)

        assertThat(result).isTrue()

        // Verify view was removed from database
        val view = mongoTemplate.findOne(
            Query(
                Criteria.where("userId").`is`(userId)
                    .and("viewableId").`is`(viewableId)
            ),
            View::class.java
        )
        assertThat(view).isNull()
    }

    @Test
    fun `should find existing view`() {
        val userId = "test-user-id"
        val viewableId = "test-viewable-id"

        // Add view to find
        viewable.addView(viewableId, userId)

        val result = viewable.findView(viewableId, userId)

        assertThat(result).isNotNull
        assertThat(result?.userId).isEqualTo(userId)
        assertThat(result?.viewableId).isEqualTo(viewableId)
    }

    @Test
    fun `should return null when finding non-existent view`() {
        val userId = "test-user-id"
        val viewableId = "test-viewable-id"

        val result = viewable.findView(viewableId, userId)

        assertThat(result).isNull()
    }
} 