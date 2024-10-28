package com.kogo.content.storage.repository.traits

import com.kogo.content.storage.entity.UserFollowing
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
class FollowableTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    private val followable: Followable = FollowableImpl(mongoTemplate)

    @BeforeEach
    fun setup() {
        mongoTemplate.dropCollection(UserFollowing::class.java)
    }

    @AfterEach
    fun cleanup() {
        mongoTemplate.dropCollection(UserFollowing::class.java)
    }

    @Test
    fun `should follow when user hasn't followed before`() {
        val userId = "test-user-id"
        val followableId = "test-followable-id"

        val result = followable.follow(followableId, userId)

        assertThat(result).isNotNull
        assertThat(result?.userId).isEqualTo(userId)
        assertThat(result?.followableId).isEqualTo(followableId)

        // Verify following exists in database
        val savedFollowing = mongoTemplate.findOne(
            Query(Criteria.where("userId").`is`(userId).and("followableId").`is`(followableId)),
            UserFollowing::class.java
        )
        assertThat(savedFollowing).isNotNull
        assertThat(savedFollowing?.userId).isEqualTo(userId)
        assertThat(savedFollowing?.followableId).isEqualTo(followableId)
    }

    @Test
    fun `should not follow when user has already followed`() {
        val userId = "test-user-id"
        val followableId = "test-followable-id"

        // Add initial following
        followable.follow(followableId, userId)

        // Try to follow again
        val result = followable.follow(followableId, userId)

        assertThat(result).isNull()

        // Verify only one following exists in database
        val followings = mongoTemplate.find(
            Query(Criteria.where("userId").`is`(userId).and("followableId").`is`(followableId)),
            UserFollowing::class.java
        )
        assertThat(followings).hasSize(1)
    }

    @Test
    fun `should unfollow when following exists`() {
        val userId = "test-user-id"
        val followableId = "test-followable-id"

        // Add initial following
        followable.follow(followableId, userId)

        val result = followable.unfollow(followableId, userId)

        assertThat(result).isTrue()

        // Verify following was removed from database
        val following = mongoTemplate.findOne(
            Query(Criteria.where("userId").`is`(userId).and("followableId").`is`(followableId)),
            UserFollowing::class.java
        )
        assertThat(following).isNull()
    }

    @Test
    fun `should return false when unfollowing non-existent following`() {
        val userId = "test-user-id"
        val followableId = "test-followable-id"

        val result = followable.unfollow(followableId, userId)

        assertThat(result).isFalse()
    }

    @Test
    fun `should unfollow all by followable id`() {
        val followableId = "test-followable-id"
        val user1Id = "test-user-1"
        val user2Id = "test-user-2"

        // Add multiple followings
        followable.follow(followableId, user1Id)
        followable.follow(followableId, user2Id)

        followable.unfollowAllByFollowableId(followableId)

        // Verify all followings were removed
        val remainingFollowings = mongoTemplate.find(
            Query(Criteria.where("followableId").`is`(followableId)),
            UserFollowing::class.java
        )
        assertThat(remainingFollowings).isEmpty()
    }

    @Test
    fun `should find existing following`() {
        val userId = "test-user-id"
        val followableId = "test-followable-id"

        // Add following to find
        followable.follow(followableId, userId)

        val result = followable.findFollowing(followableId, userId)

        assertThat(result).isNotNull
        assertThat(result?.userId).isEqualTo(userId)
        assertThat(result?.followableId).isEqualTo(followableId)
    }

    @Test
    fun `should find all followings by user id`() {
        val userId = "test-user-id"
        val followableId1 = "test-followable-1"
        val followableId2 = "test-followable-2"

        // Add multiple followings for the user
        followable.follow(followableId1, userId)
        followable.follow(followableId2, userId)

        val results = followable.findAllFollowingsByUserId(userId)

        assertThat(results).hasSize(2)
        assertThat(results.map { it.followableId }).containsExactlyInAnyOrder(followableId1, followableId2)
    }

    @Test
    fun `should find all followings by followable id`() {
        val followableId = "test-followable-id"
        val user1Id = "test-user-1"
        val user2Id = "test-user-2"

        // Add multiple followings for the followable
        followable.follow(followableId, user1Id)
        followable.follow(followableId, user2Id)

        val results = followable.findAllFollowingsByFollowableId(followableId)

        assertThat(results).hasSize(2)
        assertThat(results.map { it.userId }).containsExactlyInAnyOrder(user1Id, user2Id)
    }
}
