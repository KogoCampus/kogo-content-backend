package com.kogo.content.endpoint.controller

import com.kogo.content.endpoint.common.ErrorCode
import com.kogo.content.endpoint.common.HttpJsonResponse
import com.kogo.content.endpoint.model.*
import com.kogo.content.exception.ResourceNotFoundException
import com.kogo.content.logging.Logger
import com.kogo.content.service.PostService
import com.kogo.content.service.UserContextService
import com.kogo.content.service.TopicService
import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("media")
class TopicController @Autowired constructor(
    private val topicService : TopicService,
    private val postService: PostService,
    private val userContextService: UserContextService,
) {
    companion object : Logger()

    @GetMapping("topics/{id}")
    @Operation(
        summary = "return a topic info",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun getTopic(@PathVariable("id") topicId: String) = run {
        val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
        HttpJsonResponse.successResponse(TopicResponse.from(topic, createTopicUserActivityResponse(topic)))
    }

    @RequestMapping(
        path = ["topics"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = TopicDto::class))])
    @Operation(
        summary = "create a new topic",
        requestBody = RequestBody(),
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun createTopic(@Valid topicDto: TopicDto): ResponseEntity<*> = run {
        if (topicService.isTopicExist(topicDto.topicName))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "topic name must be unique: ${topicDto.topicName}")
        val topic = topicService.create(topicDto, userContextService.getCurrentUserDetails())
        HttpJsonResponse.successResponse(TopicResponse.from(topic, createTopicUserActivityResponse(topic)))
    }

    @RequestMapping(
        path = ["topics/{id}"],
        method = [RequestMethod.PUT],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = TopicUpdate::class))])
    @Operation(
        summary = "update topic attributes",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "ok",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(mediaType = "application/json", schema = Schema(example = "{ \"reason\": \"USER_IS_NOT_OWNER\"}"))]
            )
        ])
    fun updateGroup(
        @PathVariable("id") topicId: String,
        @Valid topicUpdate: TopicUpdate): ResponseEntity<*> = run {
            val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
            val user = userContextService.getCurrentUserDetails()

            if(!topicService.isTopicOwner(topic, userContextService.getCurrentUserDetails()))
                return HttpJsonResponse.errorResponse(ErrorCode.USER_ACTION_DENIED, "topic is not owned by user ${user.id}")

            if (topicUpdate.topicName != null && topicService.isTopicExist(topicUpdate.topicName!!))
                return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "topic name must be unique: ${topicUpdate.topicName}")
            val updatedTopic = topicService.update(topic, topicUpdate)
            HttpJsonResponse.successResponse(TopicResponse.from(updatedTopic, createTopicUserActivityResponse(topic)))
    }

    @DeleteMapping("topics/{id}")
    @Operation(
        summary = "delete a topic",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "ok",
            ),
            ApiResponse(
                responseCode = "403",
                content = [Content(mediaType = "application/json", schema = Schema(example = "{ \"reason\": \"USER_IS_NOT_OWNER\"}"))]
            )
        ])
    fun deleteTopic(@PathVariable("id") topicId: String): ResponseEntity<*> {
        val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
        val user = userContextService.getCurrentUserDetails()

        if(!topicService.isTopicOwner(topic, user))
           return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "topic is not owned by user ${user.id}")

        val deletedTopic = topicService.delete(topic)
        return HttpJsonResponse.successResponse(deletedTopic)
    }

    @RequestMapping(
        path = ["topics/{id}/follow"],
        method = [RequestMethod.PUT]
    )
    @Operation(
        summary = "follow a topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun followTopic(@PathVariable("id") topicId: String): ResponseEntity<*> = run {
        val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
        val user = userContextService.getCurrentUserDetails()

        if (topicService.isUserFollowingTopic(topic, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The user is already following the topic")

        topicService.follow(topic, user)
        HttpJsonResponse.successResponse(TopicResponse.from(topic, createTopicUserActivityResponse(topic)), "User's follow added successfully to topic: $topicId")
    }

    @RequestMapping(
        path = ["topics/{id}/unfollow"],
        method = [RequestMethod.PUT]
    )
    @Operation(
        summary = "unfollow a topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TopicResponse::class))],
        )])
    fun unfollowTopic(@PathVariable("id") topicId: String): ResponseEntity<*> = run {
        val topic = topicService.find(topicId) ?: findTopicByIdOrThrow(topicId)
        val user = userContextService.getCurrentUserDetails()

        if(topicService.isTopicOwner(topic, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The owner cannot unfollow the topic")

        if (!topicService.isUserFollowingTopic(topic, user))
            return HttpJsonResponse.errorResponse(ErrorCode.BAD_REQUEST, "The user is not following the topic")

        topicService.unfollow(topic, user)
        HttpJsonResponse.successResponse(TopicResponse.from(topic, createTopicUserActivityResponse(topic)), "User's follow successfully removed from topic: $topicId")
    }

    @GetMapping("topics/{topicId}/posts")
    @Operation(
        summary = "return a list of posts in the given topic",
        parameters = [
            Parameter(
                name = PaginationRequest.PAGE_TOKEN_PARAM,
                description = "page token",
                schema = Schema(type = "string"),
                required = false
            ),
            Parameter(
                name = PaginationRequest.PAGE_SIZE_PARAM,
                description = "limit for pagination",
                schema = Schema(type = "integer", defaultValue = "10"),
                required = false
            )],
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            headers = [Header(name = "next_page", schema = Schema(type = "string"))],
            content = [Content(mediaType = "application/json", array = ArraySchema(
                schema = Schema(implementation = PostResponse::class))
            )],
        )])
    fun listPostsInTopic(
        @PathVariable("topicId") topicId: String, @RequestParam requestParameters: Map<String, String>): ResponseEntity<*> = run {
        val paginationRequest = PaginationRequest.resolveFromRequestParameters(requestParameters)

        val paginationResponse = postService.listPostsByTopicId(topicId, paginationRequest)

        HttpJsonResponse.successResponse(
            data = paginationResponse.items.map { PostResponse.from(it, createPostUserActivityResponse(it)) },
            headers = paginationResponse.toHttpHeaders()
        )
    }

    @RequestMapping(
        path = ["topics/{topicId}/posts"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @RequestBody(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = Schema(implementation = PostDto::class))])
    @Operation(
        summary = "create a post under the given topic",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PostResponse::class))],
        )])
    fun createPostToTopic(
        @PathVariable("topicId") topicId: String,
        @Valid postDto: PostDto): ResponseEntity<*> {
        val topic = findTopicByIdOrThrow(topicId)
        val user = userContextService.getCurrentUserDetails()

        if (!topicService.isUserFollowingTopic(topic, user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not following topic id: ${topic.id}")

        val post = postService.create(topic, userContextService.getCurrentUserDetails(), postDto)
        return HttpJsonResponse.successResponse(PostResponse.from(post, createPostUserActivityResponse(post)))
    }

    @DeleteMapping("topics/{topicId}/posts/{postId}")
    @Operation(
        summary = "delete a post",
        responses = [ApiResponse(
            responseCode = "200",
            description = "ok",
        )])
    fun deletePostInTopic(
        @PathVariable("topicId") topicId: String,
        @PathVariable("postId") postId: String
    ): ResponseEntity<*> {
        val topic = findTopicByIdOrThrow(topicId)
        val post = postService.find(postId) ?: findPostByIdOrThrow(postId)
        val user = userContextService.getCurrentUserDetails()

        if(!postService.isPostAuthor(post, user) && !topicService.isTopicOwner(topic, user))
            return HttpJsonResponse.errorResponse(errorCode = ErrorCode.USER_ACTION_DENIED, "user is not the author of this post or the topic owner")

        val deletedPost = postService.delete(post)
        return HttpJsonResponse.successResponse(deletedPost)
    }


    private fun findTopicByIdOrThrow(topicId: String): Topic = topicService.find(topicId) ?: throw ResourceNotFoundException.of<Topic>(topicId)
    private fun findPostByIdOrThrow(postId: String): Post = postService.find(postId) ?: throw ResourceNotFoundException.of<Post>(postId)

    private fun createPostUserActivityResponse(post: Post): PostResponse.PostUserActivity {
        val like = postService.findLike(post, userContextService.getCurrentUserDetails())
        val view = postService.findView(post, userContextService.getCurrentUserDetails())
        return PostResponse.PostUserActivity(
            liked = like != null,
            likedAt = like?.createdAt,
            viewed = view != null,
            viewedAt = view?.createdAt,
        )
    }

    private fun createTopicUserActivityResponse(topic: Topic): TopicResponse.TopicUserActivity {
        val following = topicService.findUserFollowing(topic, userContextService.getCurrentUserDetails())
        return TopicResponse.TopicUserActivity(
            followed = following != null,
            followedAt = following?.createdAt
        )
    }
}
