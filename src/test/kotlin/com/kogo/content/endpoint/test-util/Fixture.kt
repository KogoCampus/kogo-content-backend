package com.kogo.content.endpoint.`test-util`

import com.kogo.content.storage.entity.*
import com.kogo.content.storage.view.*
import java.time.Instant
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType

inline fun <reified T : Any> fixture(noinline overrides: () -> Map<String, Any?> = { emptyMap() }): T =
    mountClass(T::class.constructors, overrides)

fun <T : Any> fixture(kClass: KClass<T>, overrides: () -> Map<String, Any?> = { emptyMap() }): T =
    mountClass(kClass.constructors, overrides)

fun <T : Any> mountClass(
    constructors: Collection<KFunction<T>>,
    overrides: () -> Map<String, Any?> = { emptyMap() },
): T {
    val ktSerializationArgName = "serializationConstructorMarker"

    val ktSerializationArg = constructors.first().parameters.firstOrNull { it.name == ktSerializationArgName }
    val constructor = if (ktSerializationArg == null) constructors.first() else constructors.last()

    val parameters = constructor.parameters
        .map { parameter ->
            if (parameter.name in overrides()) overrides()[parameter.name] else getRandomParameterValue(parameter.type)
        }

    return constructor.call(*parameters.toTypedArray())
}

private fun getRandomParameterValue(type: KType): Any? {
    val classifier = type.classifier as? KClass<*>
    return when {
        classifier == String::class -> ('a'..'z').map { it }.shuffled().subList(0, 6).joinToString("")
        classifier == Char::class -> ('a'..'z').random()
        classifier == Boolean::class -> Random.nextBoolean()
        classifier == Int::class -> Random.nextInt()
        classifier == Long::class -> Random.nextLong()
        classifier == Short::class -> Random.nextInt().toShort()
        classifier == Byte::class -> Random.nextInt().toByte()
        classifier == Float::class -> Random.nextFloat()
        classifier == Double::class -> Random.nextDouble()
        classifier == Instant::class -> Instant.now()
        classifier == List::class -> {
            val elementType = type.arguments.firstOrNull()?.type
                ?: throw IllegalArgumentException("Cannot process types without type arguments: $type")
            List(1) { getRandomParameterValue(elementType) }
        }
        classifier?.java?.isEnum == true -> {
            val enumConstants = classifier.java.enumConstants
            enumConstants[Random.nextInt(enumConstants.size)]
        }
        else -> null
    }
}

class Fixture {
    companion object {
        fun createUserFixture() = fixture<User>()

        fun createTopicFixture(owner: User? = null) = fixture<Topic> {
            mapOf(
                "owner" to (owner ?: createUserFixture())
            )
        }

        fun createPostFixture(topic: Topic, author: User? = null) = fixture<Post> {
            mapOf(
                "topic" to topic,
                "author" to (author ?: createUserFixture()),
                "attachments" to emptyList<Any>(),
            )
        }

        fun createCommentFixture(post: Post, author: User? = null) = fixture<Comment> {
            mapOf(
                "post" to post,
                "author" to (author ?: createUserFixture()),
            )
        }

        fun createReplyFixture(comment: Comment, author: User? = null) = fixture<Reply> {
            mapOf(
                "comment" to comment,
                "author" to (author ?: createUserFixture()),
            )
        }

        fun createPostAggregateFixture(post: Post) = PostAggregate(
            postId = post.id!!,
            post = post,
            viewCount = Random.nextInt(0, 1000),
            likeCount = Random.nextInt(0, 1000),
            commentCount = Random.nextInt(0, 1000),
            popularityScore = Random.nextDouble(0.0, 100.0),
            lastUpdated = Instant.now()
        )

        fun createTopicAggregateFixture(topic: Topic) = TopicAggregate(
            topicId = topic.id!!,
            topic = topic,
            followerIds = (1..Random.nextInt(0, 100)).map { "user$it" }.toSet(),
            followerCount = Random.nextInt(0, 1000),
            postCount = Random.nextInt(0, 1000),
            lastUpdated = Instant.now()
        )

        fun createCommentAggregateFixture(
            comment: Comment? = null,
            likeCount: Int = Random.nextInt(0, 1000),
            replyCount: Int = Random.nextInt(0, 1000)
        ) = CommentAggregate(
            commentId = comment?.id ?: "comment-${Random.nextInt(1000)}",
            comment = comment ?: createCommentFixture(
                createPostFixture(
                    createTopicFixture(createUserFixture())
                )
            ),
            likeCount = likeCount,
            replyCount = replyCount,
            likedUserIds = (1..likeCount).map { "user-$it" }.toSet(),
            lastUpdated = Instant.now()
        )

        fun createReplyAggregateFixture(reply: Reply) = ReplyAggregate(
            replyId = reply.id!!,
            reply = reply,
            likeCount = Random.nextInt(0, 1000),
            lastUpdated = Instant.now()
        )
    }
}
