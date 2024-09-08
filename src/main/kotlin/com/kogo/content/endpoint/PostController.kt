package com.kogo.content.endpoint

import com.kogo.content.endpoint.common.ApiResponse
import com.kogo.content.endpoint.model.PostDto
import com.kogo.content.service.PostService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("media")
class PostController @Autowired constructor(
    private val postService : PostService,
) {
    @GetMapping("groups/{groupId}/posts")
    fun getPosts(
        @PathVariable("groupId") groupId: String,
    ): ApiResponse {
        return ApiResponse.success(postService.findPostsbyGroupId(groupId))
    }

    @GetMapping("groups/{groupId}/posts/{postId}")
    fun getPost(
        @PathVariable("groupId") groupId: String,
        @PathVariable("postId") postId: String,
    ) : ApiResponse {
        return ApiResponse.success(postService.find(groupId, postId))
    }

    @RequestMapping(
        path = ["groups/{groupId}/posts"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    ) fun createPost(
        @PathVariable("groupId") groupId: String,
        @Valid postDto: PostDto
    ) : ApiResponse {
        val post = postService.create(groupId, postDto)
        return ApiResponse.success(post)
    }

    @RequestMapping(
        path = ["groups/{groupId}/posts/{postId}"],
        method = [RequestMethod.PUT]
    ) fun updatePost(
        @PathVariable("groupId") groupId: String,
        @PathVariable("postId") postId: String,
        @RequestPart("title", required = false) title: String?,
        @RequestPart("content", required = false) content: String?,
    ) : ApiResponse {

        val attributes = mapOf(
            "title" to title,
            "content" to content,
        ).filterValues { it != null }

        if (attributes.isEmpty())
            throw IllegalArgumentException("Empty request body")

        return ApiResponse.success(postService.update(groupId, postId, attributes))
    }

    @DeleteMapping("posts/{postId}")
    fun deletePost(@PathVariable("postId") postId: String) = ApiResponse.success(postService.delete(postId))

    @RequestMapping(
        path = ["posts/{postId}/attachments"],
        method = [RequestMethod.POST],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    ) fun uploadAttachments(
        @PathVariable("postId") postId: String,
        @RequestPart("attachments", required = false) attachments: List<MultipartFile>?,
    ) : ApiResponse {
        return ApiResponse.success(attachments?.forEach { attachment ->
            postService.addAttachment(postId, attachment)
        })
    }

    @DeleteMapping("posts/{postId}/attachments/{attachmentId}")
    fun deleteAttachment(
        @PathVariable("postId") postId: String,
        @PathVariable("attachmentId") attachmentId: String
    ) : ApiResponse {
        return ApiResponse.success(postService.deleteAttachment(postId, attachmentId))
    }
}